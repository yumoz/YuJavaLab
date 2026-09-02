# 测试体系

## 概述

项目使用 **JUnit 4.13.2**，测试代码位于 `src/test/java/com/example/`，按层划分为两级测试：

| 测试类 | 级别 | 用例数 | 覆盖对象 |
|---|---|---|---|
| [`UserMapperTest`](../src/test/java/com/example/mapper/UserMapperTest.java) | Mapper 级 | 4 | 直接使用 `UserMapper` |
| [`UserServiceTest`](../src/test/java/com/example/service/UserServiceTest.java) | Service 级 | 3 | 使用 `UserService` |

## 数据库初始化

两个测试类都在 `@BeforeClass` 静态方法中调用 `DatabaseInit.init()`，确保在**整个测试类运行前**建好表结构和种子数据：

```java
@BeforeClass
public static void initDb() {
    DatabaseInit.init();
}
```

由于 `init.sql` 开头有 `DROP TABLE IF EXISTS user`，每次测试类运行都会重置数据，保证测试的幂等性。

## 测试分层差异

### Mapper 级测试（UserMapperTest）

直接操作 Mapper，自行管理 SqlSession：

```java
@Before
public void setUp() {
    session = SqlSessionFactoryUtil.getSqlSessionFactory().openSession(true);
    userMapper = session.getMapper(UserMapper.class);
}

@After
public void tearDown() {
    if (session != null) {
        session.close();
    }
}
```

覆盖用例：

1. `selectById_shouldReturnUserWhenIdExists` — 查询存在的 ID 返回 zhangsan。
2. `selectById_shouldReturnNullWhenIdNotExists` — 查询不存在的 ID 返回 null。
3. `selectAll_shouldReturnAllUsers` — 查询全部用户，数量 ≥ 2。
4. `insertUser_shouldBackfillId` — 插入后主键回填到 `user.getId()`。

### Service 级测试（UserServiceTest）

使用 `UserServiceImpl`，验证业务层完整逻辑（含会话生命周期管理）：

```java
private final UserService userService = new UserServiceImpl();
```

覆盖用例：

1. `selectById_shouldReturnUser` — 按 ID 查询返回 zhangsan。
2. `selectAll_shouldReturnNonEmptyList` — 查询全部返回非空列表。
3. `insertUser_shouldPersistAndBackfillId` — 插入持久化且主键回填。

## 数据隔离策略

插入类测试使用 **UUID 短前缀**生成唯一测试数据，并在 `finally` 块中清理，避免污染种子数据和其他用例：

```java
String suffix = UUID.randomUUID().toString().substring(0, 8);
User user = new User("testuser_" + suffix, "pass123", suffix + "@example.com");
try {
    int rows = userMapper.insertUser(user);
    assertEquals(1, rows);
} finally {
    if (user.getId() != null) {
        userMapper.deleteById(user.getId());
    }
}
```

- 唯一用户名/邮箱通过 UUID 保证不冲突（邮箱有 UNIQUE 约束）。
- `finally` 保证无论断言成败都清理数据。
- 清理依赖 `deleteById`，这也是 Mapper 中提供该方法的原因。

## 运行测试

```bash
mvn test                # 运行全部测试
mvn verify              # 测试 + Checkstyle + SpotBugs
```