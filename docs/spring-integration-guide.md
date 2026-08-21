# 从裸 MyBatis 演进到 Spring 管理（迁移指南）

本指南说明如何把当前"裸 MyBatis + 手写 Service"的结构演进为 Spring 容器管理，
同时保持现有 Mapper/XML/业务代码基本不变。适用于想让演示项目"更接近生产形态"的同学。

> 为什么当前项目没有直接用 Spring？
> 项目定位是**聚焦 MyBatis 核心机制**的学习演示：SqlSessionFactory 单例、
> 手写事务、拦截器、TypeHandler 等裸 MyBatis 行为都在 Spring 里会被"隐藏"。
> 先理解裸版，再看 Spring 帮你做了什么，是更好的学习路径。

## 1. 依赖

```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-context</artifactId>
    <version>6.1.x</version>
</dependency>
<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis-spring</artifactId>
    <version>3.0.x</version>
</dependency>
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-jdbc</artifactId>
    <version>6.1.x</version>
</dependency>
```

## 2. 配置类（替代 mybatis-config.xml + SqlSessionFactoryUtil 单例）

```java
@Configuration
@MapperScan("com.example.mapper")          // 扫描 Mapper 接口，注册为 Bean
@EnableTransactionManagement                // 开启声明式事务 @Transactional
public class MyBatisConfig {

    @Bean
    public DataSource dataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setDriverClassName("org.sqlite.JDBC");
        ds.setJdbcUrl("jdbc:sqlite:yujavalab.db");
        ds.setMaximumPoolSize(5);
        return ds;
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setTypeHandlers(new TypeHandler[]{ new UserTypeTypeHandler() });
        // 插件：PageHelper + CreateTimeInterceptor
        factory.setPlugins(new Interceptor[]{
                new PageInterceptor(), new CreateTimeInterceptor()
        });
        return factory.getObject();
    }

    @Bean
    public PlatformTransactionManager txManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
```

关键点：
- `mybatis-spring` 的 `SqlSessionFactoryBean` 替我们管理了 `SqlSessionFactory` 的构建；
- `@MapperScan` 会把每个 Mapper 接口注册成代理 Bean，**Service 里直接 `@Autowired UserMapper` 即可，不再自己开/关 SqlSession**；
- 拦截器（PageHelper、CreateTimeInterceptor）、TypeHandler 从 XML 迁移到 `factory.setPlugins(...)` / `factory.setTypeHandlers(...)`。

## 3. Service 改造：注入 Mapper + 声明式事务

```java
@Service
public class AccountService {
    @Autowired
    private AccountMapper accountMapper;

    @Transactional(rollbackFor = Exception.class)
    public void transfer(Integer fromId, Integer toId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BizException("转账金额必须为正数");
        }
        Account from = accountMapper.selectById(fromId);
        Account to = accountMapper.selectById(toId);
        if (from == null || to == null) throw new BizException("账户不存在");
        if (from.getBalance().compareTo(amount) < 0) throw new BizException("余额不足");
        if (accountMapper.debit(fromId, amount) == 0) {
            throw new BizException("余额不足（并发扣减保护）");
        }
        accountMapper.credit(toId, amount);
    }
}
```

对比：
- 手写版：`SqlSessionTemplate.executeInTransaction(...)` 手动 commit/rollback；
- Spring 版：`@Transactional` 由 `DataSourceTransactionManager` 代理，方法正常返回自动提交、抛异常自动回滚。
  事务边界从"代码块"升级为"方法"，这是两者最核心的区别。

## 4. 现有代码的迁移对照

| 现有组件 | Spring 版对应 |
|---|---|
| `SqlSessionFactoryUtil`（DCL 单例） | `SqlSessionFactoryBean` + `@MapperScan` |
| `SqlSessionTemplate.execute` | 直接 `@Autowired Mapper` 调用 |
| `SqlSessionTemplate.executeInTransaction` | `@Transactional` 方法 |
| `mybatis-config.xml` settings/mappers | `SqlSessionFactoryBean` 属性 |
| `new UserServiceImpl()`（MyBatisDemo 内） | `@Autowired` / `ApplicationContext.getBean` |

Mapper 接口与 XML、实体、TypeHandler、拦截器**完全不用改**。

## 5. 学习建议

1. 先在本项目理解：SqlSessionFactory 是什么、为什么 Service 每次操作要开一个 Session、事务为什么必须手动提交；
2. 迁移后观察：`@MapperScan` 代理帮你省掉了哪些样板；
3. 深入阅读 `mybatis-spring` 的 `SqlSessionTemplate` 源码——你会发现它和我们手写的
   `com.example.util.SqlSessionTemplate` 思路一致，只是多了异常转换与事务同步（`SpringManagedTransaction`）。

## 6. 已知取舍

- SQLite 对 `SELECT ... FOR UPDATE` 不友好（文件级锁），并发演示仍以原子 UPDATE 为主；
- 若上 Spring Boot，`mybatis-spring-boot-starter` 可自动装配上述全部 Bean，配置进一步缩减到 `application.yml`。
