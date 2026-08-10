# MyBatis 面试深化 10 项 — 设计文档

- 日期：2026-08-10
- 范围：高优先级（动态 SQL、`#{}` vs `${}`、事务、缓存、多表关联）+ 中优先级（分页、拦截器、原理文档、连接池、TypeHandler）
- 明确排除：Spring Boot Web 层、乐观锁、逻辑删除、统一异常处理（用户选择不纳入本次）

## 1. 背景

`java_mybatis` 是单模块 Maven 学习项目（Java 24、MyBatis 3.5.13、SQLite、JUnit 4、无 Lombok），当前仅演示单表 CRUD。作为 Java 面试准备项目，缺乏 MyBatis 面试高频考点：动态 SQL、事务、缓存、多表关联、分页、插件机制、TypeHandler、连接池、原理理解。

## 2. 硬约束（不得破坏）

- `init.sql` 必须保留 `DROP TABLE IF EXISTS user` + 重建 + 种子 `zhangsan`(id=1)/`lisi`。
- 现有测试断言：`selectById(1).getUsername() == "zhangsan"`；`selectAll().size() >= 2`。schema 变更后这些必须仍然成立。
- 保持分层架构与现有代码风格（无 Lombok、手写 getter/setter、service 层持有 SqlSession 生命周期、SLF4J+Logback）。

## 3. Schema 变更

文件：`src/main/resources/db/init.sql`

- `user` 表新增列 `user_type INTEGER NOT NULL DEFAULT 0`（0=NORMAL 普通、1=ADMIN 管理员、2=VIP）。
- 新表 `orders`：`id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL, order_no TEXT NOT NULL, amount REAL NOT NULL, create_time DATETIME DEFAULT CURRENT_TIMESTAMP`。
- 新表 `account`：`id INTEGER PRIMARY KEY AUTOINCREMENT, account_no TEXT NOT NULL UNIQUE, name TEXT NOT NULL, balance REAL NOT NULL DEFAULT 0`。
- 种子数据：zhangsan(id=1) 挂 2 条订单；`account` 两条（如 10001/zhangsan/1000.00、10002/lisi/500.00）。

## 4. 依赖（pom.xml）

- `com.zaxxer:HikariCP` 5.1.0 → 替换 MyBatis `POOLED`。
- `com.github.pagehelper:pagehelper` 5.3.3 → 分页插件。
- `org.mockito:mockito-core` 4.x → 单测。

版本取当前主流稳定版；若与 Java 24 / MyBatis 3.5.13 冲突，以能编译通过为准。

## 5. 特性落点

### 5.1 动态 SQL + `#{}` vs `${}`

`UserMapper.xml` 新增：
- `selectByCondition`：`<where>` + `<if>`（username/password/email 可选）+ `<choose>/<when>/<otherwise>`（按 userType 过滤演示）。
- `updateSelective`：`<set>` + `<if>` 动态更新非空字段。
- `selectByConditionOrdered`：`ORDER BY ${orderBy}` 动态排序列（`${}` 场景）。

新增 `com.example.entity.UserQuery` DTO（username/email/userType/orderBy 等可选字段）。

service 层：`selectByConditionOrdered` 传入排序列前做**白名单校验**（仅允许 id/username/email/create_time），演示如何安全使用 `${}`。

### 5.2 事务

新增表 `account` + `Account` 实体 + `AccountMapper`(xml) + `AccountService`/`AccountServiceImpl`。

`AccountService.transfer(fromId, toId, amount)`：编程式事务
- `openSession(false)` → 查询双方余额 → 校验余额充足 → 扣减/增加 → `commit()`；异常 `rollback()` 后 rethrow。
- 抛出业务异常（余额不足）演示回滚。

### 5.3 二级缓存

`UserMapper.xml` 增加 `<cache/>`（eviction=LRU，默认配置即可，可加注释说明）。配合 5.6 的拦截器与 5.4 的查询观察缓存行为。

`docs/mybatis-internals.md` 中解释一级缓存（SqlSession 级别、何时失效）与二级缓存（跨 SqlSession、命名空间级别、脏读场景）。

### 5.4 多表关联

- `Order` 实体（id, userId, orderNo, amount, createTime）+ `OrderMapper`(xml)。
- `User` 实体新增 `private List<Order> orders`（getter/setter），用于 `<collection>` 映射。
- `UserMapper.xml` 新增 `selectUserWithOrders`：`LEFT JOIN orders` + `<resultMap>` 嵌套 `<collection>`，演示一对多。
- `OrderMapper.xml` 提供 `selectByUserId`、`selectById` 等基本查询。

### 5.5 分页

- 手写：`UserMapper.selectPage(offset, limit)`，返回 `List<User>`；service 组合出 Page 信息。
- PageHelper：service 层 `PageHelper.startPage(pageNum, pageSize)` + `selectAll`，返回 `PageInfo<User>`。
- 两种方式都接入 demo 菜单与测试，便于对比。

### 5.6 拦截器

`com.example.interceptor.CreateTimeInterceptor`：
- `@Intercepts(Executor.update)`，`Invocation` 反射获取参数对象，若对象含 `createTime` 属性且为 null 则填充当前时间（覆盖 `User`/`Order`；`Account` 无该字段，自动跳过）。
- 注册进 `mybatis-config.xml` `<plugins>`（与 PageHelper 并列）。

### 5.7 连接池

`mybatis-config.xml` 数据源 `type` 改为 `com.zaxxer.hikari.HikariDataSource`：
- properties：`driverClassName=org.sqlite.JDBC`、`jdbcUrl=jdbc:sqlite:yujavalab.db`、`maximumPoolSize=5`。
- MyBatis 通过 `UnpooledDataSourceFactory` 反射 setter 注入属性（Hikari 的 setter 为 camelCase）。

### 5.8 TypeHandler

- `com.example.entity.UserType` 枚举：`NORMAL(0)`, `ADMIN(1)`, `VIP(2)`，含 `code` 与 `static fromCode(int)`。
- `com.example.typehandler.UserTypeTypeHandler extends BaseTypeHandler<UserType>`：`setNonNullParameter` 写 `code`，读取时 `fromCode`。
- 注册到 `mybatis-config.xml` `<typeHandlers>`；`User` 实体加 `userType` 字段；`UserMapper.xml` resultMap 与 insert 映射 `user_type`。
- 现有 `User` 构造函数不动，默认 `NORMAL`。

### 5.9 原理文档

新增 `docs/mybatis-internals.md`（中文）：
- SqlSessionFactory 构建流程
- Mapper 接口 → JDK 动态代理（MapperProxy）原理
- Executor / StatementHandler / ParameterHandler / ResultSetHandler 四大对象职责
- 一级/二级缓存机制与失效场景
- 插件（Interceptor）拦截原理（四大对象代理 + Plugin 包装）
- `#{}`/`${}` 与预编译原理

## 6. 演示入口

扩展 `MyBatisDemo` 菜单（保持 0 退出、交互式 stdin）：
- 动态条件查询（用户名/邮箱过滤）
- 分页查询（PageHelper）
- 账户转账（事务演示）
- 用户 + 订单（多表）
- 订单查询

## 7. 测试

- 新增：`OrderMapperTest`、`AccountServiceTest`（含转账成功/余额不足回滚）、`UserService` 动态查询/分页用例、`UserQuery` 白名单用例。
- Mockito 单元测试：`UserTypeTypeHandlerTest`（mock `ResultSet`/`PreparedStatement`）、`CreateTimeInterceptorTest`（mock `MappedStatement`/`Executor`）。
- 验证命令：`mvn test`（全量）、`mvn verify`（含 checkstyle/spotbugs，非阻断）。
- 现有 `UserMapperTest`/`UserServiceTest` 必须保持绿色。

## 8. 验收标准

1. `mvn clean verify` 通过（checkstyle/spotbugs 为警告不阻断）。
2. 所有现有 + 新增测试通过。
3. 10 项特性均可通过 demo 菜单或测试观察到效果。
4. `docs/mybatis-internals.md` 完成，可作为面试讲稿素材。
