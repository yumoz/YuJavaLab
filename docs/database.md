# 数据库设计

## 概述

项目使用 **SQLite** 作为嵌入式数据库，无需启动独立服务。数据库文件 `yujavalab.db` 在应用首次启动时自动创建，并由 [`DatabaseInit.init()`](../src/main/java/com/example/util/DatabaseInit.java) 负责初始化表结构与种子数据。

## 连接配置

数据库连接信息在两处保持一致：

- [`DatabaseInit.java`](../src/main/java/com/example/util/DatabaseInit.java)：`jdbc:sqlite:yujavalab.db`
- [`mybatis-config.xml`](../src/main/resources/mybatis-config.xml)：数据源 URL 同为 `jdbc:sqlite:yujavalab.db`

## 表结构

`init.sql` 定义唯一的 `user` 表：

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT | 自增主键 |
| `username` | TEXT | NOT NULL | 用户名 |
| `password` | TEXT | NOT NULL DEFAULT '123456' | 密码，默认值 123456 |
| `email` | TEXT | UNIQUE DEFAULT '' | 邮箱，唯一约束 |
| `create_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

```sql
CREATE TABLE user (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL,
    password TEXT NOT NULL DEFAULT '123456',
    email TEXT UNIQUE DEFAULT '',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

## 初始化流程

[`init.sql`](../src/main/resources/db/init.sql) 的完整执行顺序：

1. `DROP TABLE IF EXISTS user` — 幂等清除旧表（每次启动重置）。
2. `CREATE TABLE user ...` — 创建表结构。
3. `INSERT INTO user ...` — 写入两条种子数据（`zhangsan`、`lisi`）。

### DatabaseInit.init() 执行逻辑

[`DatabaseInit.java`](../src/main/java/com/example/util/DatabaseInit.java) 使用 JDBC 原生 API 执行初始化：

1. 通过 `DriverManager.getConnection` 建立连接。
2. 从 classpath 读取 `/db/init.sql` 全文到字符串。
3. 按分号 `;` 拆分 SQL 语句，逐条 `stmt.execute()` 执行。
4. 记录 `INFO` 日志表示初始化成功，失败抛 `RuntimeException`。

> 注意：SQL 按 `;` 拆分，因此 SQL 语句内部不能包含分号字面量。

## 字段映射策略

数据库字段 `create_time`（下划线命名）与实体属性 `createTime`（驼峰命名）不一致，项目**未启用** MyBatis 的 `mapUnderscoreToCamelCase` 全局开关，而是在 [`UserMapper.xml`](../src/main/resources/mapper/UserMapper.xml) 中通过显式 `<resultMap>` 完成映射：

```xml
<resultMap id="UserResultMap" type="com.example.entity.User">
    <id column="id" property="id"/>
    <result column="username" property="username"/>
    <result column="password" property="password"/>
    <result column="email" property="email"/>
    <result column="create_time" property="createTime"/>  <!-- 下划线转驼峰 -->
</resultMap>
```

## 主键回填

`insertUser` 使用 `useGeneratedKeys="true" keyProperty="id"`，配合 SQLite 的 `AUTOINCREMENT`，插入后主键会回填到实体对象的 `id` 属性：

```xml
<insert id="insertUser" useGeneratedKeys="true" keyProperty="id">
    INSERT INTO user (username, password, email)
    VALUES (#{username}, #{password}, #{email})
</insert>
```

## 批量导入数据格式

项目提供了 [`data/users.csv`](../data/users.csv) 作为文件导入的样例，格式为三列：

```
username,password,email
file_user1,111111,file_user1@example.com
```

入口层的"从文件批量导入"功能会跳过表头行，逐行解析为 `User` 对象后调用 `insertBatch`。