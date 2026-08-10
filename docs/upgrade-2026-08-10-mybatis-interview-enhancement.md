# MyBatis 面试深化升级 — 功能介绍

> 升级日期：2026-08-10
> 设计文档：`docs/superpowers/specs/2026-08-10-mybatis-interview-enhancement-design.md`
> 实施计划：`docs/superpowers/plans/2026-08-10-mybatis-interview-enhancement.md`
> 原理速查：`docs/mybatis-internals.md`

本次升级为 `java_mybatis`（Java 24 + MyBatis 3.5.13 + SQLite）补齐了 10 个 MyBatis 面试高频考点，并把单表 CRUD 扩展成覆盖事务、缓存、多表、分页、插件、TypeHandler、连接池的完整学习项目。**所有代码基于三层架构扩展，现有测试与种子数据（`zhangsan` id=1）保持不变。**

## 快速验证

```bash
mvn clean verify        # 21 个测试 + checkstyle(0 违规) + spotbugs(无告警)
mvn exec:java           # 交互菜单，选项 5-9 体验新特性
```

## 10 项特性一览

### 1. 动态 SQL
- 位置：`UserMapper.xml` — `selectByCondition`（`<where>` + `<if>` + `<choose>`）、`updateSelective`（`<set>`）
- 面试点：动态拼接的原理（OGNL 判断 + SQL 片段拼接）、`<where>` 自动去前导 `AND`
- 演示：菜单 5「动态条件查询」；测试 `UserServiceTest#selectByCondition_shouldFilterByUsername`

### 2. `#{}` vs `${}`
- 位置：`UserMapper.xml#selectByConditionOrdered` 用 `ORDER BY ${orderBy}`；`UserServiceImpl.ORDER_BY_WHITELIST` 白名单
- 面试点：`#{}` 预编译占位防注入；`${}` 用于列名/表名/排序等无法参数绑定的场景，**必须白名单校验**
- 演示：测试 `UserServiceTest#selectByConditionOrdered_shouldRejectIllegalColumn`（注入 payload 被拒绝）

### 3. 编程式事务
- 位置：`AccountServiceImpl.transfer()` — `openSession(false)` + `commit()` / 异常 `rollback()` 后 rethrow
- 面试点：事务边界、回滚时机、`SqlSession` 非线程安全（每次调用新建 session）
- 演示：菜单 7「账户转账」；测试 `AccountServiceTest`（成功转账 + 余额不足回滚）

### 4. 二级缓存
- 位置：`UserMapper.xml` 顶部 `<cache eviction="LRU" flushInterval="60000" size="512" readOnly="true"/>`
- 面试点：一级缓存（SqlSession 级）vs 二级缓存（namespace 级）；`readOnly=true` 免序列化；insert/update/delete 自动清缓存；多表脏读风险
- 详见：`docs/mybatis-internals.md` 第 5/6 节

### 5. 多表关联（一对多）
- 位置：`orders` 表 + `Order`/`OrderMapper`；`UserMapper.xml#selectUserWithOrders` 用 `<collection>` 嵌套映射
- 面试点：`LEFT JOIN` + 嵌套 resultMap、`extends` 复用、一对多行折叠
- 演示：菜单 8「查看用户+订单」、菜单 9「查看订单」；测试 `OrderMapperTest`

### 6. 分页（两种方式）
- 位置：手写 `UserMapper.selectPage`（`LIMIT ? OFFSET ?`）；PageHelper `UserServiceImpl.selectPageByHelper`（`PageHelper.startPage` + `PageInfo`）
- 面试点：PageHelper 原理（拦截 `Executor.query` 改写 SQL + 自动 count）、`startPage` 只对下一次查询生效、线程内调用
- 演示：菜单 6「分页查询」；测试 `UserServiceTest#selectPage*`

### 7. 插件 / 拦截器
- 位置：`interceptor/CreateTimeInterceptor` — `@Intercepts(Executor.update)`，insert 时自动填充 `createTime`
- 面试点：四大对象（Executor/StatementHandler/ParameterHandler/ResultSetHandler）创建时被 `Plugin.wrap` 动态代理、`@Signature` 匹配、洋葱模型责任链
- 测试：`CreateTimeInterceptorTest`（Mockito mock Executor/MappedStatement）

### 8. HikariCP 连接池
- 位置：`datasource/HikariDataSourceFactory`（继承 `UnpooledDataSourceFactory`）+ `mybatis-config.xml`
- 面试点：`<dataSource type>` 必须是 `DataSourceFactory` 实现；`UnpooledDataSourceFactory` 用 MetaObject 反射把属性映射到 setter（camelCase 属性名 = setter 名）
- 配置：`driverClassName`/`jdbcUrl`/`maximumPoolSize=5`

### 9. 自定义 TypeHandler
- 位置：`typehandler/UserTypeTypeHandler` + `entity/UserType` 枚举（`NORMAL(0)/ADMIN(1)/VIP(2)`）+ `User.userType` 字段，全局注册于 `<typeHandlers>`
- 面试点：BaseTypeHandler 四个方法、与内置 EnumTypeHandler（存 name）/ EnumOrdinalTypeHandler（存 ordinal，重命名即错位）的取舍
- 测试：`UserTypeTypeHandlerTest`（Mockito mock ResultSet/PreparedStatement）

### 10. MyBatis 原理文档
- 位置：`docs/mybatis-internals.md` — SqlSessionFactory 构建、Mapper 动态代理、四大对象、`#{}`/`${}`、一级/二级缓存、插件原理、TypeHandler、连接池工厂、分页，每节附「本项目落点」
- 用途：面试讲稿素材，直接对应本项目代码

## Schema 变更（`src/main/resources/db/init.sql`）

| 表 | 说明 |
|---|---|
| `user` | 新增 `user_type INTEGER NOT NULL DEFAULT 0`（TypeHandler 演示） |
| `orders`（新） | `user_id/order_no/amount/create_time`，种子 3 条（zhangsan 挂 2 条） |
| `account`（新） | `account_no/name/balance`，种子 2 条（1000 / 500） |

每次运行/测试都会被 `DatabaseInit.init()` DROP 重建 + 重置种子，测试依赖 `zhangsan`(id=1)、订单、余额。

## 新增依赖（pom.xml）

- `com.zaxxer:HikariCP:5.1.0` — 连接池
- `com.github.pagehelper:pagehelper:5.3.3` — 分页插件
- `org.mockito:mockito-core:5.23.0`（test）— 单测；需 ≥5.23 以支持本机 JDK 25 的 byte-buddy
- `spotbugs-maven-plugin` 4.8.6.6 → **4.10.3.0** — 旧版无法解析 Java 24/25 class 文件

## 测试矩阵（共 21 个）

| 测试类 | 覆盖 |
|---|---|
| `UserMapperTest` (4) | CRUD + 种子数据（不变） |
| `UserServiceTest` (8) | 动态查询、白名单、`<set>` 更新、手写/PageHelper 分页 |
| `OrderMapperTest` (3) | 一对多查询、订单 CRUD |
| `AccountServiceTest` (2) | 转账成功、余额不足回滚（`@FixMethodOrder`） |
| `UserTypeTypeHandlerTest` (3) | Mockito 单测 |
| `CreateTimeInterceptorTest` (1) | Mockito 单测 |

## 交互菜单（`MyBatisDemo`，`mvn exec:java`）

新增选项 5-9：动态条件查询 / 分页查询 / 账户转账 / 用户+订单 / 订单查询。

## 踩坑与设计决策

1. **HikariCP 不能直接写 `<dataSource type="com.zaxxer.hikari.HikariDataSource">`** — MyBatis 要求 `DataSourceFactory` 接口实现，需自定义工厂。
2. **Mockito 版本**：本机运行 JDK 25（pom 目标 24），mockito ≤5.16 的 byte-buddy 无法 instrument JDK 25 class 文件，需 ≥5.23。
3. **JUnit 4 方法执行顺序非确定**：`AccountServiceTest` 必须 `@FixMethodOrder(NAME_ASCENDING)`，否则回滚用例先跑会因余额状态断言失败。
4. **`${orderBy}` 白名单**：service 层校验后才进 `${}`，防止 SQL 注入（面试可讲）。
