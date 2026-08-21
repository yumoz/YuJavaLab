# java_mybatis

一个遵循标准 Java 工程结构、单模块 Maven 的 MyBatis 学习演示项目，把面试高频知识点全部落地为**可运行、可测试**的代码：
动态 SQL、`#{}` vs `${}` 与防注入、程序化事务、一/二级缓存、一对多映射、两种分页、插件拦截器、自定义 TypeHandler、连接池、主键回填、统一业务异常。

## 技术栈

- Java 24 · Maven
- ORM: MyBatis 3.5.13
- 数据库: SQLite（免服务器，自动创建，URL 可配置）
- 连接池: HikariCP 5.1
- 分页插件: PageHelper 5.3.3
- 日志: SLF4J + Logback（org.apache.ibatis 开 DEBUG 可看动态 SQL/分页改写/缓存命中）
- 测试: JUnit 4.13.2 + Mockito 5
- 代码质量: Checkstyle + SpotBugs
- CI: GitHub Actions（SQLite 内嵌，无需数据库服务）+ Dependabot

## 功能特性（演示的 MyBatis 知识点）

| 知识点 | 落地位置 | 说明 |
|---|---|---|
| 动态 SQL | `UserMapper.xml` | `<where>/<if>/<choose>/<set>/<foreach>`：条件查询、动态更新、批量插入 |
| `#{}` vs `${}` | `selectByConditionOrdered` | 参数一律 `#{}` 预编译；`${orderBy}` 排序列由 Service 白名单强制校验，防 SQL 注入 |
| 程序化事务 | `AccountServiceImpl.transfer` | `openSession(false)` + commit/rollback；金额正数校验；**原子扣减**（`debit`/`credit` SQL）防并发超扣与丢更新 |
| 二级缓存 | `UserMapper.xml` `<cache readOnly="true"/>` | `CacheTest` 演示跨 SqlSession 命中与 insert 后失效 |
| 一对多映射 | `selectUserWithOrders` `<collection>` | LEFT JOIN 一次查出用户 + 订单列表 |
| 分页 | `selectPage` + PageHelper | 手写 `LIMIT/OFFSET` 与 `PageHelper.startPage` + `PageInfo` 两种方式对比 |
| 插件/拦截器 | `CreateTimeInterceptor` | 拦截 `Executor.update`，反射自动填充 `createTime` |
| 自定义 TypeHandler | `UserTypeTypeHandler` | 枚举 `UserType` ↔ 数据库 int 码（全局注册） |
| 连接池 | `HikariDataSourceFactory` | 自定义 MyBatis `DataSourceFactory` 接入 HikariCP |
| 主键回填 | `useGeneratedKeys="true" keyProperty="id"` | insert 后自动回填自增主键 |
| 统一业务异常 | `BizException` | 业务规则违背（余额不足/账户不存在/参数非法）与系统异常区分 |
| 金额精度 | `Account.balance: BigDecimal` | 金额一律 BigDecimal，避免浮点误差（教学最佳实践） |

## 快速开始

```bash
mvn clean compile   # 编译
mvn exec:java       # 运行（交互式控制台菜单，需在交互终端执行）
mvn test            # 全部测试
mvn verify          # 测试 + Checkstyle + SpotBugs
```

## 命令速查

```bash
mvn compile                     # 编译
mvn exec:java                   # 运行（交互菜单：CRUD/导入/动态查询/分页/转账/多表）
mvn test                        # 测试
mvn test -Dtest=UserMapperTest  # 单测类
mvn verify                      # 完整验证
mvn checkstyle:check            # 代码规范检查（warning 不阻断）
mvn spotbugs:check              # 静态缺陷检测（warning 不阻断）
mvn mybatis-generator:generate  # 从数据库表生成代码（overwrite=false）
```

## 项目结构

```
├── pom.xml
├── checkstyle.xml
├── .github/workflows/maven.yml  # CI：SQLite 内嵌 + JDK 24
├── src/main/java/com/example/
│   ├── MyBatisDemo.java                # 控制台演示入口（10 个功能菜单）
│   ├── entity/                         # User, UserType(enum), UserQuery, Order, Account(BigDecimal)
│   ├── mapper/                         # UserMapper/OrderMapper/AccountMapper 接口（XML 在 resources/mapper）
│   ├── service/ + impl/                # 三层业务层（接口 + 实现）
│   ├── exception/BizException.java     # 统一业务异常
│   ├── interceptor/CreateTimeInterceptor.java
│   ├── typehandler/UserTypeTypeHandler.java
│   ├── datasource/HikariDataSourceFactory.java
│   └── util/
│       ├── SqlSessionFactoryUtil.java  # SqlSessionFactory 单例（DCL，注入 app.db.url）
│       ├── SqlSessionTemplate.java     # SqlSession 生命周期模板（自动提交 / 事务两种模式）
│       └── DatabaseInit.java           # 建库初始化（URL 可配置）
├── src/main/resources/
│   ├── mybatis-config.xml              # HikariCP + 3 个 mapper + TypeHandler + PageHelper + 拦截器 + logImpl
│   ├── logback.xml
│   ├── db/init.sql                     # 建表 + 种子数据（zhangsan/lisi、订单、账户 1000/500）
│   ├── mapper/                         # UserMapper.xml / OrderMapper.xml / AccountMapper.xml
│   └── generator/generatorConfig.xml
├── src/test/java/com/example/
│   ├── mapper/                         # UserMapperTest, OrderMapperTest, CacheTest(二级缓存)
│   ├── service/                        # UserServiceTest, AccountServiceTest(每用例重建库)
│   ├── typehandler/UserTypeTypeHandlerTest
│   └── interceptor/CreateTimeInterceptorTest
├── data/users.csv                      # 批量导入演示数据
├── docs/                               # mybatis-internals.md 面试笔记、spring-integration-guide.md 演进指南
└── config/spotbugs-exclude.xml
```

## 数据库说明

- 默认 `jdbc:sqlite:yujavalab.db`（首次运行自动创建，`*.db` 已 gitignore）。
- **URL 可配置**：系统属性 `app.db.url` 覆盖；`SqlSessionFactoryUtil` 构建时注入 `mybatis-config.xml` 的 `${app.db.url}` 占位符。
- **测试隔离**：surefire 将测试进程固定为 `jdbc:sqlite:target/test.db`，与主库隔离，`mvn clean` 自动清除。
- 每次运行/测试 `DatabaseInit.init()` 重建表 + 种子数据，**不要期望状态跨运行保留**。

## 测试说明

- Mapper 级：`UserMapperTest`、`OrderMapperTest`、`CacheTest`（二级缓存命中/失效）。
- Service 级：`UserServiceTest`、`AccountServiceTest`——每个用例 `@Before` 重建库，**无顺序依赖**（转账测试含：成功、余额不足回滚、负数/零金额拒绝、账户不存在回滚）。
- 单元：`UserTypeTypeHandlerTest`、`CreateTimeInterceptorTest`（Mockito）。
- 全部 DB 测试依赖 `init.sql` 种子数据（`selectById(1)` = zhangsan；账户 1=1000 / 账户 2=500），修改种子行会破坏测试。

## 安全与教学说明

- 密码**明文存储仅为教学演示**，生产应使用 BCrypt/Argon2 等加盐哈希。
- 金额使用 `BigDecimal`；转账通过原子 `UPDATE ... WHERE balance >= #{amount}` 防并发超扣。
- `${}` 只用于白名单列名，严禁拼接用户输入（`UserServiceImpl.ORDER_BY_WHITELIST`）。
- `readOnly="true"` 二级缓存会缓存对象引用：实体请保持只读使用，勿把可变引用写回缓存。

## 相关文档

- `AGENTS.md` — 开发工作指引（含约定与已知事项）
- `docs/mybatis-internals.md` — MyBatis 面试笔记
- `docs/spring-integration-guide.md` — 从裸 MyBatis 演进到 Spring 管理的指南
- `docs/superpowers/specs/` — 设计文档
