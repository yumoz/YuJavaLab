# MyBatis 核心原理速查（面试讲稿）

配合本项目的 `UserMapper`/`OrderMapper`/`AccountMapper` 阅读。每节的"本项目落点"告诉你代码在哪。

## 1. SqlSessionFactory 构建流程

- `SqlSessionFactoryBuilder.build(inputStream)` 解析 `mybatis-config.xml` → `XMLConfigBuilder` → `Configuration` 对象。
- 依次注册 typeAliases、typeHandlers、plugins、environments、mappers。
- 每个 `<mapper>` 通过 `XMLMapperBuilder` 解析 XML，把每条 SQL 编译成 `MappedStatement`（一条语句一个）。
- 最终 `Configuration` 被 `DefaultSqlSessionFactory` 持有。

**本项目落点**：`util/SqlSessionFactoryUtil.java`（DCL 单例构建）；`resources/mybatis-config.xml`。

## 2. Mapper 接口代理原理

- 接口本身无法实例化。MyBatis 在 `Configuration.addMapper` 时注册 `MapperProxyFactory`。
- `sqlSession.getMapper(UserMapper.class)` → JDK 动态代理，代理类实现 `InvocationHandler`（`MapperProxy`）。
- `MapperProxy.invoke` 把方法（含 `@Param` 签名）解析为对应的 `MappedStatement`（key = `namespace + "." + 方法名`）→ 交给 `SqlSession` 执行。
- 所以 Mapper 接口没有实现类，XML 的 `namespace` 必须等于接口全限定名。

**本项目落点**：`mapper/UserMapper.java` ↔ `resources/mapper/UserMapper.xml`（namespace `com.example.mapper.UserMapper`）。

## 3. 四大对象（一次查询的执行链）

1. `Executor`（`CachingExecutor` / `BaseExecutor`）—— 缓存、事务、SQL 执行调度；插件最常拦截它。
2. `StatementHandler` —— 创建 `PreparedStatement`、绑定参数、执行。
3. `ParameterHandler` —— 把 Java 参数写入 `PreparedStatement`（走 TypeHandler）。
4. `ResultSetHandler` —— 把 `ResultSet` 行映射回 Java 对象（走 resultMap / TypeHandler）。

**本项目落点**：`interceptor/CreateTimeInterceptor` 拦截的就是 `Executor.update`。

## 4. `#{}` vs `${}`

- `#{}`：预编译占位符。`ParameterHandler` 用 TypeHandler 写参数，SQL 结构不变 → 防 SQL 注入。
- `${}`：直接把字符串拼进 SQL（动态列名/表名/排序方向）。有注入风险，只能拼接白名单。

**本项目落点**：`UserMapper.xml#selectByConditionOrdered` 用 `ORDER BY ${orderBy}`；service 层 `UserServiceImpl.ORDER_BY_WHITELIST` 白名单校验（`id/username/email/create_time`）。

## 5. 一级缓存（Local Cache）

- 默认开启，作用域 = 单个 `SqlSession`。
- 同一 session 内同语句同参数二次查询直接返回缓存（不查库）。
- 失效时机：session 关闭/换 session、执行 update/insert/delete、手动 `clearCache()`、查询语句或参数不同、`localCacheScope=STATEMENT`。
- 本项目每次 service 调用都用 try-with-resources 新建 session → 一级缓存基本不跨调用生效（面试可讲"为什么 MyBatis 一级缓存不适合跨请求复用"）。

## 6. 二级缓存（namespace 级）

- 开启：`<cache/>`；作用域 = 命名空间，跨 SqlSession 共享（同一 SqlSessionFactory）。
- `readOnly="true"`：直接缓存对象引用，免序列化（本项目 User 未实现 Serializable）。
- 刷新：该命名空间任何 update/insert/delete 自动清空。
- 风险：多表查询脏读——只在 UserMapper 声明 cache 时，orders 表更新不会刷新 user 命名空间缓存。面试高频题。

**本项目落点**：`UserMapper.xml` 顶部 `<cache eviction="LRU" flushInterval="60000" size="512" readOnly="true"/>`。

## 7. 插件（Interceptor）原理

- 四大对象创建时经 `InterceptorChain.pluginAll()` 包装 → `Plugin.wrap` 生成 JDK 动态代理。
- 只有方法签名命中 `@Intercepts/@Signature` 才拦截，否则放行。
- 多个插件按注册顺序嵌套包装（洋葱模型，`proceed()` 逐层向下）。

**本项目落点**：
- `CreateTimeInterceptor`：拦 `Executor.update`，反射填充 `createTime`。
- PageHelper 的 `PageInterceptor`：拦 `Executor.query`，改写 SQL 加 `LIMIT` 并先执行 count。

## 8. TypeHandler

- 职责：Java 类型 ↔ JDBC 类型互转。
- 全局注册于 `<typeHandlers>`，MyBatis 按 javaType 自动匹配。
- 内置 `EnumTypeHandler`（存枚举 name 字符串）、`EnumOrdinalTypeHandler`（存 ordinal，重命名枚举会错位）。

**本项目落点**：`typehandler/UserTypeTypeHandler` 继承 `BaseTypeHandler<UserType>`，存 `code` int、读 int→枚举（`UserType.fromCode`）。比 ordinal 抗重命名，比 name 存字符串省空间。

## 9. 连接池与数据源工厂

- MyBatis `<dataSource type>` 必须是 `DataSourceFactory` 实现（内部 Class.cast 校验）。
- 自定义工厂继承 `UnpooledDataSourceFactory`，构造器换掉 `this.dataSource`；`setProperties` 用 MetaObject 反射把属性映射到 setter（camelCase 属性名 = setter 名）。

**本项目落点**：`datasource/HikariDataSourceFactory`；`mybatis-config.xml` 中 `driverClassName/jdbcUrl/maximumPoolSize` 分别对应 Hikari 的 `setDriverClassName/setJdbcUrl/setMaximumPoolSize`。

## 10. 分页

- 手写分页：`LIMIT #{limit} OFFSET #{offset}`（本项目 `UserMapper.selectPage`）。
- PageHelper：`PageHelper.startPage(pageNum, pageSize)` 后紧跟一次查询，`PageInterceptor` 改写 SQL + 自动 count，返回 `PageInfo`（total/pageNum/...）。注意：startPage 只作用于下一次查询，且要求线程内调用。

**本项目落点**：`UserServiceImpl.selectPageManually` / `selectPageByHelper`。
