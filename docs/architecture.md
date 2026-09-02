# 架构设计

## 概述

本项目是一个遵循标准 Java 工程结构的 MyBatis CRUD 学习 Demo，采用经典三层分层架构，清晰地划分了数据访问、业务逻辑和入口交互的职责边界。

## 分层架构

```
┌─────────────────────────────────────────┐
│        入口层（Entry Point）             │
│   MyBatisDemo  — 菜单循环 + 用户交互     │
└────────────────────┬────────────────────┘
                     │ 依赖
┌────────────────────▼────────────────────┐
│        业务层（Service）                 │
│   UserService（接口）                    │
│   UserServiceImpl（实现，管理 SqlSession）│
└────────────────────┬────────────────────┘
                     │ 依赖
┌────────────────────▼────────────────────┐
│        数据访问层（Mapper）              │
│   UserMapper（接口）+ UserMapper.xml     │
└────────────────────┬────────────────────┘
                     │ 依赖
┌────────────────────▼────────────────────┐
│        实体层（Entity）                  │
│   User  — POJO，手写 getter/setter      │
└─────────────────────────────────────────┘
```

### 各层职责

| 层 | 类 | 职责 |
|---|---|---|
| 入口层 | `MyBatisDemo` | 交互式菜单、控制台输入、CSV 文件导入 |
| 业务层 | `UserService` / `UserServiceImpl` | 编排事务、管理 SqlSession 生命周期 |
| 数据访问层 | `UserMapper` / `UserMapper.xml` | 定义 SQL 语句与参数/结果映射 |
| 实体层 | `User` | 数据载体，映射 `user` 表 |

## 调用链路

以"查询所有用户"为例：

```
用户选择菜单「1. 查询所有用户」
  └─ MyBatisDemo.main → userService.selectAll()
       └─ UserServiceImpl.selectAll()
            ├─ SqlSessionFactoryUtil.getSqlSessionFactory()  // 获取单例工厂
            ├─ factory.openSession(true)                     // 开启会话（自动提交）
            ├─ session.getMapper(UserMapper.class)           // 获取 Mapper 代理
            ├─ mapper.selectAll()                            // 执行 SQL
            └─ try-with-resources 关闭 SqlSession
```

## SqlSession 生命周期

会话由 **Service 层**统一管理，每个方法使用 try-with-resources 确保 `SqlSession` 及时关闭：

```java
try (SqlSession session = SqlSessionFactoryUtil.getSqlSessionFactory().openSession(true)) {
    UserMapper mapper = session.getMapper(UserMapper.class);
    return mapper.selectAll();
}
```

- **开启**：`openSession(true)`，`true` 表示启用自动提交。
- **关闭**：try-with-resources 在方法返回或抛异常时自动调用 `session.close()`。
- **不跨层传递**：SqlSession 不向上暴露到入口层，业务层是唯一持有者。

## SqlSessionFactory 单例（DCL）

[`SqlSessionFactoryUtil`](../src/main/java/com/example/util/SqlSessionFactoryUtil.java) 使用双重检查锁定（Double-Checked Locking）保证全局只有一个工厂实例：

```java
private static volatile SqlSessionFactory sqlSessionFactory;

public static SqlSessionFactory getSqlSessionFactory() {
    if (sqlSessionFactory == null) {                     // 第一次检查（无锁）
        synchronized (SqlSessionFactoryUtil.class) {
            if (sqlSessionFactory == null) {             // 第二次检查（持锁）
                sqlSessionFactory = new SqlSessionFactoryBuilder()
                    .build(Resources.getResourceAsStream("mybatis-config.xml"));
            }
        }
    }
    return sqlSessionFactory;
}
```

要点：
- `volatile` 修饰防止指令重排序导致的半初始化状态对外可见。
- `SqlSessionFactory` 构建代价高，只构建一次并复用。
- 私有构造方法禁止外部实例化。

## 入口交互

[`MyBatisDemo`](../src/main/java/com/example/MyBatisDemo.java) 的 `main` 方法：

1. 调用 `DatabaseInit.init()` 初始化数据库表结构与种子数据。
2. 创建 `UserServiceImpl` 实例。
3. 进入 `while(true)` 菜单循环，提供 5 个选项：
   - `1` 查询所有用户
   - `2` 根据 ID 查询用户
   - `3` 控制台新增用户（校验非空）
   - `4` 从 CSV 文件批量导入
   - `0` 退出

## 设计约定

- **不使用 Lombok**：实体类手写 getter/setter 与 `toString`。
- **Service 层持有会话生命周期**：避免连接泄漏。
- **Mapper 接口 + XML 分离**：SQL 集中在 XML 中，Java 接口只定义签名。
- **入口层只依赖 Service 接口**：不直接接触 Mapper，符合依赖倒置原则。