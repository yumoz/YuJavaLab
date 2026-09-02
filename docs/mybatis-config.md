# MyBatis 配置详解

## 核心配置 mybatis-config.xml

[`mybatis-config.xml`](../src/main/resources/mybatis-config.xml) 是 MyBatis 的全局配置文件，本项目的关键配置如下：

```xml
<configuration>
    <environments default="development">
        <environment id="development">
            <transactionManager type="JDBC"/>
            <dataSource type="POOLED">
                <property name="driver" value="org.sqlite.JDBC"/>
                <property name="url" value="jdbc:sqlite:yujavalab.db"/>
            </dataSource>
        </environment>
    </environments>
    <mappers>
        <mapper resource="mapper/UserMapper.xml"/>
    </mappers>
</configuration>
```

### 要点说明

| 配置项 | 值 | 说明 |
|---|---|---|
| `transactionManager type` | `JDBC` | 使用 JDBC 事务（依赖连接的 commit/rollback） |
| `dataSource type` | `POOLED` | MyBatis 内置连接池 |
| `driver` | `org.sqlite.JDBC` | SQLite JDBC 驱动 |
| `url` | `jdbc:sqlite:yujavalab.db` | 相对路径数据库文件 |
| `mappers` | `mapper/UserMapper.xml` | 注册 Mapper XML |

## Mapper 映射 UserMapper.xml

[`UserMapper.xml`](../src/main/resources/mapper/UserMapper.xml) 定义了 `UserMapper` 接口对应的 SQL。

### namespace 约定

```xml
<mapper namespace="com.example.mapper.UserMapper">
```

`namespace` 必须与 Mapper 接口的全限定名（FQN）完全一致，MyBatis 据此将接口方法与 SQL 语句绑定。

### resultMap 结果映射

```xml
<resultMap id="UserResultMap" type="com.example.entity.User">
    <id column="id" property="id"/>
    <result column="create_time" property="createTime"/>
    ...
</resultMap>
```

- `<id>` 标记主键，用于缓存和对象判等。
- `<result>` 完成普通列的别名映射。
- 多个 `<select>` 复用同一 `resultMap`。

### 各 SQL 语句

| 语句 ID | 类型 | 关键特性 |
|---|---|---|
| `selectById` | select | 参数 `@Param("id")`，返回 `UserResultMap` |
| `selectAll` | select | 查询全部，复用 `UserResultMap` |
| `insertUser` | insert | `useGeneratedKeys="true" keyProperty="id"` 主键回填 |
| `insertBatch` | insert | `<foreach>` 拼接多行 VALUES |
| `deleteById` | delete | 按主键删除，主要用于测试清理 |

### foreach 批量插入

`insertBatch` 使用 `<foreach>` 将 `List<User>` 展开为多条 VALUES：

```xml
<insert id="insertBatch" useGeneratedKeys="true" keyProperty="id">
    INSERT INTO user (username, password, email) VALUES
    <foreach collection="list" item="user" separator=",">
        (#{user.username}, #{user.password}, #{user.email})
    </foreach>
</insert>
```

- `collection="list"`：对应接口参数 `@Param("list") List<User> list`。
- `item="user"`：遍历元素变量名。
- `separator=","`：多条记录间用逗号分隔。

## Mapper 接口 UserMapper

[`UserMapper.java`](../src/main/java/com/example/mapper/UserMapper.java) 定义 Mapper 接口，使用 `@Param` 显式命名参数：

| 方法 | 参数 | 返回 |
|---|---|---|
| `selectById` | `@Param("id") Integer id` | `User` |
| `selectAll` | — | `List<User>` |
| `insertUser` | `User user` | `int` |
| `insertBatch` | `@Param("list") List<User>` | `int` |
| `deleteById` | `@Param("id") Integer id` | `int` |

## 代码生成器 Generator

[`generatorConfig.xml`](../src/main/resources/generator/generatorConfig.xml) 配置 MyBatis Generator，用于从数据库表反向生成实体、Mapper 接口和 XML：

```xml
<context id="sqlite" targetRuntime="MyBatis3" defaultModelType="flat">
    <jdbcConnection driverClass="org.sqlite.JDBC"
                    connectionURL="jdbc:sqlite:yujavalab.db"/>
    <javaModelGenerator targetPackage="com.example.entity" targetProject="src/main/java"/>
    <sqlMapGenerator targetPackage="mapper" targetProject="src/main/resources"/>
    <javaClientGenerator type="XMLMAPPER" targetPackage="com.example.mapper" targetProject="src/main/java"/>
    <table tableName="user" domainObjectName="User">
        <generatedKey column="id" sqlStatement="SQLite" identity="true"/>
    </table>
</context>
```

### 要点

- `targetRuntime="MyBatis3"`：标准 MyBatis 3 模式（接口 + XML，非注解）。
- `javaClientGenerator type="XMLMAPPER"`：生成 XML Mapper 而非注解版接口。
- `<generatedKey>`：声明 `id` 为 SQLite 自增主键。
- 通过 `mvn mybatis-generator:generate` 触发，插件配置 `overwrite=false`，不会覆盖已手写的文件。