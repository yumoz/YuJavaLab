# MyBatis 面试深化 10 项 — 实施计划

> **执行记录（2026-08-10，全部完成，未 git commit）：**
> 1. `mockito-core` 实际采用 **5.23.0**（非 5.16.1）——本机运行 JDK 25，5.16.1 内置 byte-buddy 1.15.11 只支持到 Java 24，无法 mock `java.sql` 接口。
> 2. Task 5 已按修正版实现（`HikariDataSourceFactory` extends `UnpooledDataSourceFactory`）。
> 3. `AccountServiceTest` 增加 `@FixMethodOrder(MethodSorters.NAME_ASCENDING)`——JUnit 4 方法执行顺序为 hash 序，回滚用例先跑会导致余额断言失败。
> 4. 顺带修复预存在问题：`spotbugs-maven-plugin` 4.8.6.6 → **4.10.3.0**（4.8.6 无法解析 Java 24/25 class 文件，`mvn verify` 会直接崩）。
> 5. 最终 `mvn clean verify`：checkstyle 0 违规、21 测试全过、spotbugs 无告警、BUILD SUCCESS。

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 `java_mybatis` 学习项目补齐 MyBatis 面试高频考点：动态 SQL、`#{}` vs `${}`、编程式事务、二级缓存、多表关联、分页（手写+PageHelper）、拦截器、HikariCP 连接池、自定义 TypeHandler、原理文档。

**Architecture:** 在现有三层架构（mapper → service → demo/测试）上扩展。新增 `orders`/`account` 表与对应实体/映射/服务；`user` 表加 `user_type` 列演示 TypeHandler；全局注册 PageHelper 与自定义拦截器；数据源切换 HikariCP。现有测试断言（`selectById(1)=zhangsan`、`selectAll().size()>=2`）必须保持。

**Tech Stack:** Java 24 · Maven · MyBatis 3.5.13 · SQLite · JUnit 4 · Mockito 5 · PageHelper 5.3.3 · HikariCP 5.x · SLF4J+Logback

**注意：** 本计划含"Commit（可选）"步骤。除非用户明确要求，执行时跳过 git commit（用户此前表示"需要提交时可以告诉我"）。

**硬约束：**
1. `src/main/resources/db/init.sql` 的 `DROP TABLE IF EXISTS user` + 重建 + 种子 `zhangsan`(id=1)/`lisi` 逻辑必须保留。
2. 不引入 Lombok；getter/setter 手写。
3. checkstyle.xml 禁用 star import（`AvoidStarImport`）、`IllegalCatch`（禁 `catch(Exception)`）。所有代码遵守。

---

### Task 1: pom.xml 新增依赖

**Files:**
- Modify: `pom.xml`（在 `<dependencies>` 中、`junit` 之前插入）

- [ ] **Step 1: 添加依赖**

在 `pom.xml` 的 `<dependencies>` 中 JUnit 依赖之前插入：

```xml
        <!-- 连接池 -->
        <dependency>
            <groupId>com.zaxxer</groupId>
            <artifactId>HikariCP</artifactId>
            <version>5.1.0</version>
        </dependency>

        <!-- 分页插件 -->
        <dependency>
            <groupId>com.github.pagehelper</groupId>
            <artifactId>pagehelper</artifactId>
            <version>5.3.3</version>
        </dependency>

        <!-- Mockito 单元测试 -->
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <version>5.16.1</version>
            <scope>test</scope>
        </dependency>
```

- [ ] **Step 2: 验证依赖解析**

Run: `mvn -q dependency:resolve`
Expected: BUILD SUCCESS，无红色 ERROR。
若 Mockito 运行时（Task 3 之后执行测试时）报 byte-buddy 类版本错误，将 `mockito-core` 升到更新版本。

- [ ] **Step 3: Commit（可选）**

```bash
git add pom.xml
git commit -m "chore: add HikariCP, PageHelper, Mockito dependencies"
```

---

### Task 2: Schema 变更（init.sql）

**Files:**
- Modify: `src/main/resources/db/init.sql`

- [ ] **Step 1: 重写 init.sql**

完整文件内容：

```sql
DROP TABLE IF EXISTS user;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS account;

CREATE TABLE user (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL,
    password TEXT NOT NULL DEFAULT '123456',
    email TEXT UNIQUE DEFAULT '',
    user_type INTEGER NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE orders (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    order_no TEXT NOT NULL,
    amount REAL NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE account (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    account_no TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    balance REAL NOT NULL DEFAULT 0
);

INSERT INTO user (username, password, email) VALUES
('zhangsan', '123456', 'zhangsan@test.com'),
('lisi', '654321', 'lisi@test.com');

INSERT INTO orders (user_id, order_no, amount) VALUES
(1, 'A1001', 199.00),
(1, 'A1002', 59.50),
(2, 'A1003', 88.00);

INSERT INTO account (account_no, name, balance) VALUES
('ACCT-10001', 'zhangsan', 1000.00),
('ACCT-10002', 'lisi', 500.00);
```

- [ ] **Step 2: 确认现有测试仍绿**

Run: `mvn -q test`
Expected: BUILD SUCCESS（`UserMapperTest`、`UserServiceTest` 全过）。user 表种子与 id 未变，`selectById(1)` 仍返回 zhangsan。

- [ ] **Step 3: Commit（可选）**

```bash
git add src/main/resources/db/init.sql
git commit -m "feat: add orders/account tables and user_type column"
```

---

### Task 3: 自定义 TypeHandler（枚举 code 映射）

**Files:**
- Create: `src/main/java/com/example/entity/UserType.java`
- Create: `src/main/java/com/example/typehandler/UserTypeTypeHandler.java`
- Create: `src/test/java/com/example/typehandler/UserTypeTypeHandlerTest.java`
- Modify: `src/main/resources/mybatis-config.xml`（注册 typeHandler）
- Modify: `src/main/java/com/example/entity/User.java`（加 `userType` 字段，默认 NORMAL）
- Modify: `src/main/resources/mapper/UserMapper.xml`（resultMap 与 insert 映射 `user_type`）

- [ ] **Step 1: 写枚举 UserType**

```java
package com.example.entity;

public enum UserType {
    NORMAL(0),
    ADMIN(1),
    VIP(2);

    private final int code;

    UserType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static UserType fromCode(int code) {
        for (UserType type : UserType.values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown UserType code: " + code);
    }
}
```

- [ ] **Step 2: 写 TypeHandler（先写失败测试）**

```java
package com.example.typehandler;

import com.example.entity.UserType;
import org.junit.Before;
import org.junit.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserTypeTypeHandlerTest {

    private UserTypeTypeHandler handler;

    @Before
    public void setUp() {
        handler = new UserTypeTypeHandler();
    }

    @Test
    public void setNonNullParameter_writesCode() throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);
        handler.setNonNullParameter(ps, 2, UserType.VIP, null);
        verify(ps).setInt(2, 2);
    }

    @Test
    public void getNullableResult_readsByColumnName() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt("user_type")).thenReturn(1);
        assertEquals(UserType.ADMIN, handler.getNullableResult(rs, "user_type"));
    }

    @Test
    public void getNullableResult_readsByIndex() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt(3)).thenReturn(0);
        assertEquals(UserType.NORMAL, handler.getNullableResult(rs, 3));
    }
}
```

- [ ] **Step 3: 运行测试确认失败（编译失败即可）**

Run: `mvn -q test -Dtest=UserTypeTypeHandlerTest`
Expected: FAIL —— `UserTypeTypeHandler` 类不存在，编译报错。

- [ ] **Step 4: 实现 TypeHandler**

```java
package com.example.typehandler;

import com.example.entity.UserType;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserTypeTypeHandler extends BaseTypeHandler<UserType> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, UserType parameter, JdbcType jdbcType) throws SQLException {
        ps.setInt(i, parameter.getCode());
    }

    @Override
    public UserType getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return UserType.fromCode(rs.getInt(columnName));
    }

    @Override
    public UserType getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return UserType.fromCode(rs.getInt(columnIndex));
    }

    @Override
    public UserType getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return UserType.fromCode(cs.getInt(columnIndex));
    }
}
```

- [ ] **Step 5: User 实体加字段**

在 `User.java` 的 `createTime` 字段后加：

```java
    private UserType userType = UserType.NORMAL;
```

并在 getter/setter 区加：

```java
    public UserType getUserType() { return userType; }
    public void setUserType(UserType userType) { this.userType = userType; }
```

无需改 import（同包 `com.example.entity`）。

- [ ] **Step 6: UserMapper.xml 映射 `user_type`**

`resultMap` 内 `create_time` 行后加：

```xml
        <result column="user_type" property="userType"/>
```

`insertUser` 的 SQL 改为（columns 加 `user_type`，values 加 `#{userType}`）：

```xml
    <insert id="insertUser" parameterType="com.example.entity.User" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO user (username, password, email, user_type)
        VALUES (#{username}, #{password}, #{email}, #{userType})
    </insert>
```

`insertBatch` 同理（foreach 内加 `#{user.userType}`）：

```xml
    <insert id="insertBatch" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO user (username, password, email, user_type) VALUES
        <foreach collection="list" item="user" separator=",">
            (#{user.username}, #{user.password}, #{user.email}, #{user.userType})
        </foreach>
    </insert>
```

`selectById`/`selectAll` 的 SELECT 列加 `user_type`：

```xml
        SELECT id, username, password, email, user_type, create_time
```

- [ ] **Step 7: mybatis-config.xml 注册 typeHandler**

在 `<configuration>` 内、`<environments>` 之前插入（DTD 顺序：settings → typeAliases → typeHandlers → objectFactory → plugins → environments）：

```xml
    <typeHandlers>
        <typeHandler handler="com.example.typehandler.UserTypeTypeHandler" javaType="com.example.entity.UserType"/>
    </typeHandlers>
```

- [ ] **Step 8: 运行 TypeHandler 测试 + 全量测试**

Run: `mvn -q test -Dtest=UserTypeTypeHandlerTest`
Expected: PASS（3 个用例）

Run: `mvn -q test`
Expected: BUILD SUCCESS（现有测试不受影响；insert 现在写入 user_type=0）

- [ ] **Step 9: Commit（可选）**

```bash
git add src/main/java/com/example/entity/UserType.java src/main/java/com/example/typehandler/UserTypeTypeHandler.java src/test/java/com/example/typehandler/UserTypeTypeHandlerTest.java src/main/java/com/example/entity/User.java src/main/resources/mapper/UserMapper.xml src/main/resources/mybatis-config.xml
git commit -m "feat: add custom UserType TypeHandler"
```

---

### Task 4: 动态 SQL + `#{}` vs `${}`

**Files:**
- Create: `src/main/java/com/example/entity/UserQuery.java`
- Modify: `src/main/java/com/example/mapper/UserMapper.java`
- Modify: `src/main/resources/mapper/UserMapper.xml`
- Modify: `src/main/java/com/example/service/UserService.java`
- Modify: `src/main/java/com/example/service/impl/UserServiceImpl.java`
- Modify: `src/test/java/com/example/service/UserServiceTest.java`

- [ ] **Step 1: UserQuery DTO**

```java
package com.example.entity;

public class UserQuery {
    private String username;
    private String email;
    private UserType userType;
    private String orderBy;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public UserType getUserType() { return userType; }
    public void setUserType(UserType userType) { this.userType = userType; }
    public String getOrderBy() { return orderBy; }
    public void setOrderBy(String orderBy) { this.orderBy = orderBy; }
}
```

- [ ] **Step 2: UserMapper.java 加方法**

```java
    // 动态条件查询（<where>/<if>/<choose> 演示）
    List<User> selectByCondition(UserQuery query);

    // 动态排序列（${} 演示，列名必须由 service 白名单校验）
    List<User> selectByConditionOrdered(UserQuery query);

    // 动态更新非空字段（<set> 演示）
    int updateSelective(User user);
```

加 import：`com.example.entity.UserQuery`。

- [ ] **Step 3: UserMapper.xml 加 SQL**

在 `</mapper>` 前插入：

```xml
    <select id="selectByCondition" resultMap="UserResultMap">
        SELECT id, username, password, email, user_type, create_time
        FROM user
        <where>
            <if test="username != null and username != ''">
                AND username LIKE '%' || #{username} || '%'
            </if>
            <if test="email != null and email != ''">
                AND email = #{email}
            </if>
            <choose>
                <when test="userType != null">
                    AND user_type = #{userType}
                </when>
                <otherwise>
                    AND user_type IS NOT NULL
                </otherwise>
            </choose>
        </where>
    </select>

    <select id="selectByConditionOrdered" resultMap="UserResultMap">
        SELECT id, username, password, email, user_type, create_time
        FROM user
        <where>
            <if test="username != null and username != ''">
                AND username LIKE '%' || #{username} || '%'
            </if>
            <if test="email != null and email != ''">
                AND email = #{email}
            </if>
        </where>
        ORDER BY ${orderBy}
    </select>

    <update id="updateSelective" parameterType="com.example.entity.User">
        UPDATE user
        <set>
            <if test="username != null">username = #{username},</if>
            <if test="password != null">password = #{password},</if>
            <if test="email != null">email = #{email},</if>
            <if test="userType != null">user_type = #{userType},</if>
        </set>
        WHERE id = #{id}
    </update>
```

注意 `#{userType}` 走 Task 3 注册的 TypeHandler 绑定枚举 code；`${orderBy}` 是字符串拼接（列名无法参数绑定），必须由 service 白名单校验。

- [ ] **Step 4: UserService 接口加方法**

```java
    List<User> selectByCondition(UserQuery query);

    List<User> selectByConditionOrdered(UserQuery query);

    int updateSelective(User user);
```

加 import：`com.example.entity.UserQuery`。

- [ ] **Step 5: UserServiceImpl 实现（含白名单）**

加字段：

```java
    private static final List<String> ORDER_BY_WHITELIST = List.of("id", "username", "email", "create_time");
```

加方法：

```java
    @Override
    public List<User> selectByCondition(UserQuery query) {
        try (SqlSession session = SqlSessionFactoryUtil.getSqlSessionFactory().openSession(true)) {
            UserMapper mapper = session.getMapper(UserMapper.class);
            return mapper.selectByCondition(query);
        }
    }

    @Override
    public List<User> selectByConditionOrdered(UserQuery query) {
        String orderBy = query.getOrderBy();
        if (orderBy == null || !ORDER_BY_WHITELIST.contains(orderBy)) {
            throw new IllegalArgumentException("非法的排序列: " + orderBy);
        }
        try (SqlSession session = SqlSessionFactoryUtil.getSqlSessionFactory().openSession(true)) {
            UserMapper mapper = session.getMapper(UserMapper.class);
            return mapper.selectByConditionOrdered(query);
        }
    }

    @Override
    public int updateSelective(User user) {
        try (SqlSession session = SqlSessionFactoryUtil.getSqlSessionFactory().openSession(true)) {
            UserMapper mapper = session.getMapper(UserMapper.class);
            return mapper.updateSelective(user);
        }
    }
```

加 import：`com.example.entity.UserQuery`。

- [ ] **Step 6: UserServiceTest 加用例**

```java
    @Test
    public void selectByCondition_shouldFilterByUsername() {
        UserQuery query = new UserQuery();
        query.setUsername("zhang");
        List<User> users = userService.selectByCondition(query);
        assertFalse(users.isEmpty());
        assertTrue(users.stream().allMatch(u -> u.getUsername().contains("zhang")));
    }

    @Test
    public void selectByConditionOrdered_shouldRejectIllegalColumn() {
        UserQuery query = new UserQuery();
        query.setOrderBy("password; DROP TABLE user; --");
        try {
            userService.selectByConditionOrdered(query);
            fail("应拒绝非白名单排序列");
        } catch (IllegalArgumentException expected) {
            assertEquals("非法的排序列: password; DROP TABLE user; --", expected.getMessage());
        }
    }

    @Test
    public void updateSelective_shouldOnlyUpdateProvidedFields() {
        User target = userService.selectById(1);
        target.setEmail("updated@test.com");
        target.setPassword(null);
        userService.updateSelective(target);

        User reloaded = userService.selectById(1);
        assertEquals("updated@test.com", reloaded.getEmail());
        assertEquals("123456", reloaded.getPassword());
    }
```

加 import：`com.example.entity.UserQuery`。

- [ ] **Step 7: 运行测试**

Run: `mvn -q test -Dtest=UserServiceTest`
Expected: PASS（原 3 用例 + 新 3 用例）

- [ ] **Step 8: Commit（可选）**

```bash
git add src/main/java/com/example/entity/UserQuery.java src/main/java/com/example/mapper/UserMapper.java src/main/resources/mapper/UserMapper.xml src/main/java/com/example/service/UserService.java src/main/java/com/example/service/impl/UserServiceImpl.java src/test/java/com/example/service/UserServiceTest.java
git commit -m "feat: dynamic SQL and safe dynamic order-by"
```

---

### Task 5: 切换 HikariCP 连接池

> **2026-08-10 修正：** MyBatis 的 `<dataSource type>` 必须是实现 `org.apache.ibatis.datasource.DataSourceFactory` 的类（内部会 Class.cast 到该接口），直接填 `com.zaxxer.hikari.HikariDataSource` 会抛 `ClassCastException`。正确做法是写一个继承 `UnpooledDataSourceFactory` 的工厂，在构造器里把 `dataSource` 换成 `new HikariDataSource()`；`UnpooledDataSourceFactory.setProperties` 会用 MetaObject 反射把属性映射到 setter（camelCase）。

**Files:**
- Create: `src/main/java/com/example/datasource/HikariDataSourceFactory.java`
- Modify: `src/main/resources/mybatis-config.xml`

- [ ] **Step 1: 创建 HikariDataSourceFactory**

```java
package com.example.datasource;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSourceFactory;

public class HikariDataSourceFactory extends UnpooledDataSourceFactory {

    public HikariDataSourceFactory() {
        this.dataSource = new HikariDataSource();
    }
}
```

- [ ] **Step 2: 替换 dataSource**

`<dataSource type="POOLED">` 整段替换为：

```xml
            <dataSource type="com.example.datasource.HikariDataSourceFactory">
                <property name="driverClassName" value="org.sqlite.JDBC"/>
                <property name="jdbcUrl" value="jdbc:sqlite:yujavalab.db"/>
                <property name="username" value=""/>
                <property name="password" value=""/>
                <property name="maximumPoolSize" value="5"/>
            </dataSource>
```

原理：`UnpooledDataSourceFactory` 用 MetaObject 反射调用 DataSource 的 setter（camelCase 属性名 = setter 名）。这也是面试可讲的点。

- [ ] **Step 3: 验证**

Run: `mvn -q test`
Expected: BUILD SUCCESS（连接池替换后现有测试全绿）

- [ ] **Step 4: Commit（可选）**

```bash
git add src/main/java/com/example/datasource/HikariDataSourceFactory.java src/main/resources/mybatis-config.xml
git commit -m "feat: switch datasource to HikariCP"
```

---

### Task 6: 多表关联（orders 一对多）

**Files:**
- Create: `src/main/java/com/example/entity/Order.java`
- Create: `src/main/java/com/example/mapper/OrderMapper.java`
- Create: `src/main/resources/mapper/OrderMapper.xml`
- Create: `src/main/java/com/example/service/OrderService.java`
- Create: `src/main/java/com/example/service/impl/OrderServiceImpl.java`
- Modify: `src/main/java/com/example/entity/User.java`（加 `orders` 集合）
- Modify: `src/main/resources/mapper/UserMapper.xml`（selectUserWithOrders + 嵌套 collection）
- Modify: `src/main/java/com/example/mapper/UserMapper.java`
- Modify: `src/main/java/com/example/service/UserService.java`
- Modify: `src/main/java/com/example/service/impl/UserServiceImpl.java`
- Modify: `src/main/resources/mybatis-config.xml`（注册 OrderMapper）
- Create: `src/test/java/com/example/mapper/OrderMapperTest.java`

- [ ] **Step 1: Order 实体**

```java
package com.example.entity;

import java.util.Date;

public class Order {
    private Integer id;
    private Integer userId;
    private String orderNo;
    private Double amount;
    private Date createTime;

    public Order() { }

    public Order(Integer userId, String orderNo, Double amount) {
        this.userId = userId;
        this.orderNo = orderNo;
        this.amount = amount;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    @Override
    public String toString() {
        return "Order{id=" + id + ", userId=" + userId + ", orderNo='" + orderNo + "', amount=" + amount + "}";
    }
}
```

- [ ] **Step 2: OrderMapper 接口**

```java
package com.example.mapper;

import com.example.entity.Order;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface OrderMapper {
    List<Order> selectByUserId(@Param("userId") Integer userId);

    Order selectById(@Param("id") Integer id);

    int insertOrder(Order order);
}
```

- [ ] **Step 3: OrderMapper.xml**

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.example.mapper.OrderMapper">
    <resultMap id="OrderResultMap" type="com.example.entity.Order">
        <id column="id" property="id"/>
        <result column="user_id" property="userId"/>
        <result column="order_no" property="orderNo"/>
        <result column="amount" property="amount"/>
        <result column="create_time" property="createTime"/>
    </resultMap>

    <select id="selectByUserId" resultMap="OrderResultMap">
        SELECT id, user_id, order_no, amount, create_time
        FROM orders
        WHERE user_id = #{userId}
        ORDER BY id
    </select>

    <select id="selectById" resultMap="OrderResultMap">
        SELECT id, user_id, order_no, amount, create_time
        FROM orders
        WHERE id = #{id}
    </select>

    <insert id="insertOrder" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO orders (user_id, order_no, amount)
        VALUES (#{userId}, #{orderNo}, #{amount})
    </insert>

    <delete id="deleteById">
        DELETE FROM orders WHERE id = #{id}
    </delete>
</mapper>
```

- [ ] **Step 4: 注册 OrderMapper.xml**

`mybatis-config.xml` 的 `<mappers>` 中加：

```xml
        <mapper resource="mapper/OrderMapper.xml"/>
```

- [ ] **Step 5: User 实体加集合 + UserMapper 加联表方法**

`User.java` 加字段与 getter/setter：

```java
    private List<Order> orders;

    public List<Order> getOrders() { return orders; }
    public void setOrders(List<Order> orders) { this.orders = orders; }
```

import 加 `java.util.List`。

`UserMapper.java` 加：

```java
    // 一对多联表查询（<collection> 演示）
    User selectUserWithOrders(@Param("id") Integer id);
```

`UserMapper.xml` 加（放在 `</mapper>` 前）：

```xml
    <resultMap id="UserWithOrdersResultMap" type="com.example.entity.User" extends="UserResultMap">
        <collection property="orders" ofType="com.example.entity.Order">
            <id column="order_id" property="id"/>
            <result column="order_no" property="orderNo"/>
            <result column="amount" property="amount"/>
            <result column="order_create_time" property="createTime"/>
        </collection>
    </resultMap>

    <select id="selectUserWithOrders" resultMap="UserWithOrdersResultMap">
        SELECT u.id, u.username, u.password, u.email, u.user_type, u.create_time,
               o.id AS order_id, o.order_no, o.amount, o.create_time AS order_create_time
        FROM user u
        LEFT JOIN orders o ON o.user_id = u.id
        WHERE u.id = #{id}
        ORDER BY o.id
    </select>
```

- [ ] **Step 6: OrderService + 实现**

```java
package com.example.service;

import com.example.entity.Order;

import java.util.List;

public interface OrderService {
    List<Order> selectByUserId(Integer userId);

    Order selectById(Integer id);
}
```

```java
package com.example.service.impl;

import com.example.entity.Order;
import com.example.mapper.OrderMapper;
import com.example.service.OrderService;
import com.example.util.SqlSessionFactoryUtil;
import org.apache.ibatis.session.SqlSession;

import java.util.List;

public class OrderServiceImpl implements OrderService {

    @Override
    public List<Order> selectByUserId(Integer userId) {
        try (SqlSession session = SqlSessionFactoryUtil.getSqlSessionFactory().openSession(true)) {
            OrderMapper mapper = session.getMapper(OrderMapper.class);
            return mapper.selectByUserId(userId);
        }
    }

    @Override
    public Order selectById(Integer id) {
        try (SqlSession session = SqlSessionFactoryUtil.getSqlSessionFactory().openSession(true)) {
            OrderMapper mapper = session.getMapper(OrderMapper.class);
            return mapper.selectById(id);
        }
    }
}
```

- [ ] **Step 7: UserService 加 selectUserWithOrders**

接口加：

```java
    User selectUserWithOrders(Integer id);
```

实现加：

```java
    @Override
    public User selectUserWithOrders(Integer id) {
        try (SqlSession session = SqlSessionFactoryUtil.getSqlSessionFactory().openSession(true)) {
            UserMapper mapper = session.getMapper(UserMapper.class);
            return mapper.selectUserWithOrders(id);
        }
    }
```

- [ ] **Step 8: OrderMapperTest**

```java
package com.example.mapper;

import com.example.entity.Order;
import com.example.util.DatabaseInit;
import com.example.util.SqlSessionFactoryUtil;
import org.apache.ibatis.session.SqlSession;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class OrderMapperTest {

    private SqlSession session;
    private OrderMapper orderMapper;

    @BeforeClass
    public static void initDb() {
        DatabaseInit.init();
    }

    @Before
    public void setUp() {
        session = SqlSessionFactoryUtil.getSqlSessionFactory().openSession(true);
        orderMapper = session.getMapper(OrderMapper.class);
    }

    @After
    public void tearDown() {
        if (session != null) {
            session.close();
        }
    }

    @Test
    public void selectByUserId_shouldReturnOrders() {
        List<Order> orders = orderMapper.selectByUserId(1);
        assertNotNull(orders);
        assertEquals(2, orders.size());
    }

    @Test
    public void selectById_shouldReturnOrder() {
        Order order = orderMapper.selectById(1);
        assertNotNull(order);
        assertEquals("A1001", order.getOrderNo());
    }

    @Test
    public void insertOrder_shouldBackfillId() {
        Order order = new Order(2, "T1001", 12.5);
        try {
            int rows = orderMapper.insertOrder(order);
            assertEquals(1, rows);
            assertNotNull(order.getId());
        } finally {
            if (order.getId() != null) {
                session.delete("com.example.mapper.OrderMapper.deleteById", order.getId());
            }
        }
    }
}
```

注意：测试没有 `deleteById` statement，用 `session.delete` 需要存在该语句。为避免新增，改用下面的做法：**Step 8 的 insertOrder 测试改为通过 service 层面验证**——直接删掉 `insertOrder_shouldBackfillId` 用例，只保留两个查询用例，插入行为由拦截器测试（Task 8）与 demo 覆盖。若想保留插入验证，在 `OrderMapper.xml` 补一个 `deleteById`：

```xml
    <delete id="deleteById">
        DELETE FROM orders WHERE id = #{id}
    </delete>
```

（默认采纳：补 `deleteById`，保留插入用例。）

- [ ] **Step 9: 运行测试**

Run: `mvn -q test -Dtest=OrderMapperTest,UserMapperTest,UserServiceTest`
Expected: PASS

Run: `mvn -q test`
Expected: BUILD SUCCESS

- [ ] **Step 10: Commit（可选）**

```bash
git add src/main/java/com/example/entity/Order.java src/main/java/com/example/mapper/OrderMapper.java src/main/resources/mapper/OrderMapper.xml src/main/java/com/example/service/OrderService.java src/main/java/com/example/service/impl/OrderServiceImpl.java src/main/java/com/example/entity/User.java src/main/resources/mapper/UserMapper.xml src/main/java/com/example/mapper/UserMapper.java src/main/java/com/example/service/UserService.java src/main/java/com/example/service/impl/UserServiceImpl.java src/main/resources/mybatis-config.xml src/test/java/com/example/mapper/OrderMapperTest.java
git commit -m "feat: one-to-many user-orders query with nested collection"
```

---

### Task 7: 事务（Account 转账）

**Files:**
- Create: `src/main/java/com/example/entity/Account.java`
- Create: `src/main/java/com/example/mapper/AccountMapper.java`
- Create: `src/main/resources/mapper/AccountMapper.xml`
- Create: `src/main/java/com/example/service/AccountService.java`
- Create: `src/main/java/com/example/service/impl/AccountServiceImpl.java`
- Create: `src/test/java/com/example/service/AccountServiceTest.java`
- Modify: `src/main/resources/mybatis-config.xml`（注册 AccountMapper）

- [ ] **Step 1: Account 实体**

```java
package com.example.entity;

public class Account {
    private Integer id;
    private String accountNo;
    private String name;
    private Double balance;

    public Account() { }

    public Account(String accountNo, String name, Double balance) {
        this.accountNo = accountNo;
        this.name = name;
        this.balance = balance;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getAccountNo() { return accountNo; }
    public void setAccountNo(String accountNo) { this.accountNo = accountNo; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Double getBalance() { return balance; }
    public void setBalance(Double balance) { this.balance = balance; }
}
```

- [ ] **Step 2: AccountMapper 接口**

```java
package com.example.mapper;

import com.example.entity.Account;
import org.apache.ibatis.annotations.Param;

public interface AccountMapper {
    Account selectById(@Param("id") Integer id);

    int updateBalance(@Param("id") Integer id, @Param("balance") Double balance);
}
```

- [ ] **Step 3: AccountMapper.xml**

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.example.mapper.AccountMapper">
    <resultMap id="AccountResultMap" type="com.example.entity.Account">
        <id column="id" property="id"/>
        <result column="account_no" property="accountNo"/>
        <result column="name" property="name"/>
        <result column="balance" property="balance"/>
    </resultMap>

    <select id="selectById" resultMap="AccountResultMap">
        SELECT id, account_no, name, balance
        FROM account
        WHERE id = #{id}
    </select>

    <update id="updateBalance">
        UPDATE account
        SET balance = #{balance}
        WHERE id = #{id}
    </update>
</mapper>
```

- [ ] **Step 4: 注册 AccountMapper.xml**

`mybatis-config.xml` 的 `<mappers>` 中加：

```xml
        <mapper resource="mapper/AccountMapper.xml"/>
```

- [ ] **Step 5: AccountService 接口**

```java
package com.example.service;

public interface AccountService {
    void transfer(Integer fromId, Integer toId, double amount);
}
```

- [ ] **Step 6: AccountServiceImpl（编程式事务）**

```java
package com.example.service.impl;

import com.example.entity.Account;
import com.example.mapper.AccountMapper;
import com.example.service.AccountService;
import com.example.util.SqlSessionFactoryUtil;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccountServiceImpl implements AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountServiceImpl.class);

    @Override
    public void transfer(Integer fromId, Integer toId, double amount) {
        try (SqlSession session = SqlSessionFactoryUtil.getSqlSessionFactory().openSession(false)) {
            AccountMapper mapper = session.getMapper(AccountMapper.class);
            try {
                Account from = mapper.selectById(fromId);
                Account to = mapper.selectById(toId);
                if (from == null || to == null) {
                    throw new IllegalArgumentException("账户不存在");
                }
                if (from.getBalance() < amount) {
                    throw new IllegalStateException("余额不足");
                }
                mapper.updateBalance(fromId, from.getBalance() - amount);
                mapper.updateBalance(toId, to.getBalance() + amount);
                log.info("transfer {} from {} to {}", amount, fromId, toId);
                session.commit();
            } catch (RuntimeException e) {
                session.rollback();
                throw e;
            }
        }
    }
}
```

- [ ] **Step 7: AccountServiceTest（含回滚验证）**

```java
package com.example.service;

import com.example.service.impl.AccountServiceImpl;
import com.example.util.DatabaseInit;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class AccountServiceTest {

    private final AccountService accountService = new AccountServiceImpl();

    @BeforeClass
    public static void initDb() {
        DatabaseInit.init();
    }

    @Test
    public void transfer_shouldMoveMoney() {
        accountService.transfer(1, 2, 100.0);
        AccountServiceTestHelper.assertBalance(1, 900.0);
        AccountServiceTestHelper.assertBalance(2, 600.0);
    }

    @Test
    public void transfer_shouldRollbackWhenInsufficientBalance() {
        try {
            accountService.transfer(2, 1, 99999.0);
            fail("余额不足应抛出异常");
        } catch (IllegalStateException expected) {
            assertEquals("余额不足", expected.getMessage());
        }
        AccountServiceTestHelper.assertBalance(2, 600.0);
        AccountServiceTestHelper.assertBalance(1, 900.0);
    }
}
```

辅助类 `AccountServiceTestHelper`（同包，读取余额）：

```java
package com.example.service;

import com.example.entity.Account;
import com.example.mapper.AccountMapper;
import com.example.util.SqlSessionFactoryUtil;
import org.apache.ibatis.session.SqlSession;

import static org.junit.Assert.assertEquals;

public final class AccountServiceTestHelper {

    private AccountServiceTestHelper() { }

    public static void assertBalance(int accountId, double expected) {
        try (SqlSession session = SqlSessionFactoryUtil.getSqlSessionFactory().openSession(true)) {
            AccountMapper mapper = session.getMapper(AccountMapper.class);
            Account account = mapper.selectById(accountId);
            assertEquals(expected, account.getBalance(), 0.001);
        }
    }
}
```

- [ ] **Step 8: 运行测试**

Run: `mvn -q test -Dtest=AccountServiceTest`
Expected: PASS（转账成功 + 余额不足回滚）

- [ ] **Step 9: Commit（可选）**

```bash
git add src/main/java/com/example/entity/Account.java src/main/java/com/example/mapper/AccountMapper.java src/main/resources/mapper/AccountMapper.xml src/main/java/com/example/service/AccountService.java src/main/java/com/example/service/impl/AccountServiceImpl.java src/test/java/com/example/service/AccountServiceTest.java src/test/java/com/example/service/AccountServiceTestHelper.java src/main/resources/mybatis-config.xml
git commit -m "feat: programmatic transaction demo with account transfer"
```

---

### Task 8: 拦截器（create_time 自动填充）+ 分页

**Files:**
- Create: `src/main/java/com/example/interceptor/CreateTimeInterceptor.java`
- Create: `src/test/java/com/example/interceptor/CreateTimeInterceptorTest.java`
- Modify: `src/main/resources/mybatis-config.xml`（注册 PageHelper + 拦截器）
- Modify: `src/main/resources/mapper/UserMapper.xml`（selectPage）
- Modify: `src/main/java/com/example/mapper/UserMapper.java`
- Modify: `src/main/java/com/example/service/UserService.java`
- Modify: `src/main/java/com/example/service/impl/UserServiceImpl.java`
- Modify: `src/test/java/com/example/service/UserServiceTest.java`

- [ ] **Step 1: 拦截器（先写失败测试）**

`CreateTimeInterceptorTest`：

```java
package com.example.interceptor;

import com.example.entity.User;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Invocation;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CreateTimeInterceptorTest {

    @Test
    public void intercept_shouldFillCreateTimeOnEntity() throws Throwable {
        User user = new User("a", "b", "c@test.com");
        assertNull(user.getCreateTime());

        Executor executor = mock(Executor.class);
        MappedStatement ms = mock(MappedStatement.class);
        when(executor.update(ms, user)).thenReturn(1);

        CreateTimeInterceptor interceptor = new CreateTimeInterceptor();
        Object result = interceptor.intercept(new Invocation(
                executor, Executor.class.getMethod("update", MappedStatement.class, Object.class),
                new Object[]{ms, user}));

        assertEquals(1, result);
        assertNotNull(user.getCreateTime());
        verify(executor).update(ms, user);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -q test -Dtest=CreateTimeInterceptorTest`
Expected: FAIL（类不存在，编译错误）

- [ ] **Step 3: 实现拦截器**

```java
package com.example.interceptor;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
public class CreateTimeInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger(CreateTimeInterceptor.class);

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object parameter = invocation.getArgs()[1];
        fillCreateTime(parameter);
        return invocation.proceed();
    }

    private void fillCreateTime(Object parameter) {
        Object target = resolveEntity(parameter);
        if (target == null) {
            return;
        }
        try {
            Field field = target.getClass().getDeclaredField("createTime");
            field.setAccessible(true);
            if (field.get(target) == null) {
                field.set(target, new Date());
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.debug("Skip createTime fill for {}", target.getClass().getSimpleName());
        }
    }

    private Object resolveEntity(Object parameter) {
        if (parameter == null) {
            return null;
        }
        if (parameter instanceof Map) {
            for (Object value : ((Map<?, ?>) parameter).values()) {
                if (value != null && value.getClass().getName().startsWith("com.example.entity.")) {
                    return value;
                }
            }
            return null;
        }
        return parameter;
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }
}
```

- [ ] **Step 4: 运行拦截器测试**

Run: `mvn -q test -Dtest=CreateTimeInterceptorTest`
Expected: PASS

- [ ] **Step 5: mybatis-config.xml 注册 PageHelper + 拦截器**

在 `<typeHandlers>` 之后、`<environments>` 之前插入 `<plugins>`（DTD 顺序允许）：

```xml
    <plugins>
        <plugin interceptor="com.github.pagehelper.PageInterceptor">
            <property name="helperDialect" value="sqlite"/>
            <property name="reasonable" value="true"/>
        </plugin>
        <plugin interceptor="com.example.interceptor.CreateTimeInterceptor"/>
    </plugins>
```

- [ ] **Step 6: 手写分页 + PageHelper 分页**

`UserMapper.java` 加：

```java
    // 手写分页（LIMIT/OFFSET）
    List<User> selectPage(@Param("offset") int offset, @Param("limit") int limit);
```

`UserMapper.xml` 加：

```xml
    <select id="selectPage" resultMap="UserResultMap">
        SELECT id, username, password, email, user_type, create_time
        FROM user
        ORDER BY id
        LIMIT #{limit} OFFSET #{offset}
    </select>
```

`UserService.java` 加：

```java
    List<User> selectPageManually(int offset, int limit);

    PageInfo<User> selectPageByHelper(int pageNum, int pageSize);
```

`UserServiceImpl.java` 加（import `com.github.pagehelper.PageHelper`、`com.github.pagehelper.PageInfo`）：

```java
    @Override
    public List<User> selectPageManually(int offset, int limit) {
        try (SqlSession session = SqlSessionFactoryUtil.getSqlSessionFactory().openSession(true)) {
            UserMapper mapper = session.getMapper(UserMapper.class);
            return mapper.selectPage(offset, limit);
        }
    }

    @Override
    public PageInfo<User> selectPageByHelper(int pageNum, int pageSize) {
        try (SqlSession session = SqlSessionFactoryUtil.getSqlSessionFactory().openSession(true)) {
            PageHelper.startPage(pageNum, pageSize);
            UserMapper mapper = session.getMapper(UserMapper.class);
            List<User> users = mapper.selectAll();
            return new PageInfo<>(users);
        }
    }
```

- [ ] **Step 7: 分页测试**

`UserServiceTest` 加：

```java
    @Test
    public void selectPageManually_shouldRespectOffsetAndLimit() {
        List<User> page = userService.selectPageManually(1, 1);
        assertEquals(1, page.size());
        assertEquals("lisi", page.get(0).getUsername());
    }

    @Test
    public void selectPageByHelper_shouldReturnPageInfo() {
        PageInfo<User> page = userService.selectPageByHelper(1, 1);
        assertEquals(1, page.getList().size());
        assertEquals(2, page.getTotal());
        assertEquals("zhangsan", page.getList().get(0).getUsername());
    }
```

import 加 `com.github.pagehelper.PageInfo`。

- [ ] **Step 8: 全量测试**

Run: `mvn -q test`
Expected: BUILD SUCCESS。
注意点：`CreateTimeInterceptor` 已生效，`insertUser`/`insertOrder` 会填充 `createTime`；DB 的 CURRENT_TIMESTAMP 默认值不再触发，但行为一致。

- [ ] **Step 9: Commit（可选）**

```bash
git add src/main/java/com/example/interceptor/CreateTimeInterceptor.java src/test/java/com/example/interceptor/CreateTimeInterceptorTest.java src/main/resources/mybatis-config.xml src/main/resources/mapper/UserMapper.xml src/main/java/com/example/mapper/UserMapper.java src/main/java/com/example/service/UserService.java src/main/java/com/example/service/impl/UserServiceImpl.java src/test/java/com/example/service/UserServiceTest.java
git commit -m "feat: create_time interceptor, manual + PageHelper pagination"
```

---

### Task 9: 二级缓存

**Files:**
- Modify: `src/main/resources/mapper/UserMapper.xml`

- [ ] **Step 1: 加 `<cache/>`**

在 `<mapper>` 标签内、`<resultMap>` 之前加：

```xml
    <cache eviction="LRU" flushInterval="60000" size="512" readOnly="true"/>
```

说明：`readOnly="true"` 直接缓存对象引用，避免 User 未实现 `Serializable` 时的序列化要求（面试可展开：readOnly=false 需 Serializable + 反序列化拷贝）。

- [ ] **Step 2: 验证**

Run: `mvn -q test`
Expected: BUILD SUCCESS。二级缓存按命名空间缓存 `UserMapper.xml` 的所有 select 结果；insert/update/delete 自动刷新该命名空间缓存。现有测试（含 insert 后回查）不受影响。

- [ ] **Step 3: Commit（可选）**

```bash
git add src/main/resources/mapper/UserMapper.xml
git commit -m "feat: enable second-level cache on user mapper"
```

---

### Task 10: MyBatisDemo 菜单扩展

**Files:**
- Modify: `src/main/java/com/example/MyBatisDemo.java`

- [ ] **Step 1: 加服务字段与菜单项**

`main` 方法开头加：

```java
        AccountService accountService = new AccountServiceImpl();
        OrderService orderService = new OrderServiceImpl();
```

菜单 `switch` 前加提示项，并在 `case "0"` 前加：

```java
            System.out.println("5. 动态条件查询");
            System.out.println("6. 分页查询（PageHelper）");
            System.out.println("7. 账户转账（事务）");
            System.out.println("8. 查看用户+订单（多表）");
            System.out.println("9. 查看订单（按用户）");
```

switch 加 case：

```java
                case "5":
                    dynamicQuery(scanner, userService);
                    break;
                case "6":
                    pageQuery(scanner, userService);
                    break;
                case "7":
                    transfer(scanner, accountService);
                    break;
                case "8":
                    userWithOrders(scanner, userService);
                    break;
                case "9":
                    ordersByUser(scanner, orderService);
                    break;
```

- [ ] **Step 2: 新增 private 方法**

```java
    private static void dynamicQuery(Scanner scanner, UserService userService) {
        System.out.print("用户名关键字（可空）：");
        String username = scanner.nextLine().trim();
        UserQuery query = new UserQuery();
        query.setUsername(username);
        List<User> users = userService.selectByCondition(query);
        System.out.println("共 " + users.size() + " 条记录：");
        users.forEach(u -> System.out.println("  " + u));
    }

    private static void pageQuery(Scanner scanner, UserService userService) {
        System.out.print("页码：");
        int pageNum = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("每页条数：");
        int pageSize = Integer.parseInt(scanner.nextLine().trim());
        PageInfo<User> page = userService.selectPageByHelper(pageNum, pageSize);
        System.out.println("第 " + page.getPageNum() + " 页，共 " + page.getTotal() + " 条：");
        page.getList().forEach(u -> System.out.println("  " + u));
    }

    private static void transfer(Scanner scanner, AccountService accountService) {
        System.out.print("转出账户ID：");
        int fromId = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("转入账户ID：");
        int toId = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("金额：");
        double amount = Double.parseDouble(scanner.nextLine().trim());
        try {
            accountService.transfer(fromId, toId, amount);
            System.out.println("转账成功");
        } catch (RuntimeException e) {
            System.out.println("转账失败：" + e.getMessage());
        }
    }

    private static void userWithOrders(Scanner scanner, UserService userService) {
        System.out.print("用户ID：");
        int id = Integer.parseInt(scanner.nextLine().trim());
        User user = userService.selectUserWithOrders(id);
        if (user == null) {
            System.out.println("用户不存在");
            return;
        }
        System.out.println(user);
        if (user.getOrders() != null) {
            user.getOrders().forEach(o -> System.out.println("  订单: " + o));
        }
    }

    private static void ordersByUser(Scanner scanner, OrderService orderService) {
        System.out.print("用户ID：");
        int userId = Integer.parseInt(scanner.nextLine().trim());
        List<Order> orders = orderService.selectByUserId(userId);
        System.out.println("共 " + orders.size() + " 条订单：");
        orders.forEach(o -> System.out.println("  " + o));
    }
```

- [ ] **Step 3: 补 import**

`MyBatisDemo.java` import 增加：

```java
import com.example.entity.Order;
import com.example.entity.UserQuery;
import com.example.service.AccountService;
import com.example.service.OrderService;
import com.example.service.impl.AccountServiceImpl;
import com.example.service.impl.OrderServiceImpl;
import com.github.pagehelper.PageInfo;
```

（`User`、`List` 已 import。）

- [ ] **Step 4: 编译验证**

Run: `mvn -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit（可选）**

```bash
git add src/main/java/com/example/MyBatisDemo.java
git commit -m "feat: extend console demo menu"
```

---

### Task 11: 原理文档 + AGENTS.md 同步

**Files:**
- Create: `docs/mybatis-internals.md`
- Modify: `AGENTS.md`

- [ ] **Step 1: 写原理文档**

`docs/mybatis-internals.md` 内容（中文，作为面试讲稿素材）：

```markdown
# MyBatis 核心原理速查（面试讲稿）

## 1. SqlSessionFactory 构建流程
- `SqlSessionFactoryBuilder.build(inputStream)` 解析 `mybatis-config.xml` → `XMLConfigBuilder` → `Configuration` 对象
- 注册 typeAliases、typeHandlers、plugins、environments、mappers
- 每个 `<mapper>` 通过 `XMLMapperBuilder` 解析 XML 与注解，构建 `MappedStatement`（一条 SQL 一个）
- 最终由 `DefaultSqlSessionFactory` 持有 `Configuration`

## 2. Mapper 接口代理原理
- 接口本身不能实例化。MyBatis 在 `Configuration.addMapper` 时注册 `MapperProxyFactory`
- `sqlSession.getMapper(UserMapper.class)` → JDK 动态代理：`MapperProxy` 拦截方法调用
- `MapperProxy.invoke` → 把方法解析为对应的 `MappedStatement`（按 namespace + 方法 id）→ 交给 `SqlSession` 执行
- 所以 Mapper 接口没有实现类，XML 的 `namespace` 必须等于接口 FQN

## 3. 四大对象（一次查询的执行链）
1. `Executor`（CachingExecutor/BaseExecutor）—— 负责缓存、事务、SQL 执行调度；插件常拦截它
2. `StatementHandler` —— 创建 `PreparedStatement`、绑定参数、执行
3. `ParameterHandler` —— 把 Java 参数写入 `PreparedStatement`（走 TypeHandler）
4. `ResultSetHandler` —— 把 `ResultSet` 行映射回 Java 对象（走 resultMap/TypeHandler）

## 4. `#{}` vs `${}`
- `#{}`：预编译占位符，`ParameterHandler` 用 TypeHandler 写参数 → 防 SQL 注入
- `${}`：直接字符串拼接进 SQL（动态列名/表名/排序）→ 有注入风险，只能拼白名单
- SQLite 的 `LIKE '%' || #{x} || '%'` 是字符串拼接写法（不同数据库语法不同）

## 5. 一级缓存（Local Cache）
- 默认开启，作用域 = 单个 SqlSession
- 同一 SqlSession 内同语句同参数二次查询直接返回缓存
- 失效时机：SqlSession 关闭/不同 session、执行 update/insert/delete（清空该 session 缓存）、手动 `clearCache()`、查询条件或参数不同、`localCacheScope=STATEMENT`（关）
- 本项目每次 service 调用都用 try-with-resources 新建 session → 一级缓存基本不跨调用生效

## 6. 二级缓存（namespace 级）
- 开启：`<cache/>`；作用域 = 命名空间，跨 SqlSession 共享（同一 SqlSessionFactory）
- readOnly=true：直接缓存对象引用（本项目 User 未实现 Serializable）
- 刷新：该命名空间任何 update/insert/delete 自动清空
- 风险：多表查询缓存脏读（只在一个命名空间声明 cache 时，另一张表的更新不刷新它）——面试高频

## 7. 插件（Interceptor）原理
- 四大对象创建时会被 `InterceptorChain.pluginAll()` 包装 → `Plugin.wrap` 生成 JDK 动态代理
- 只有方法签名被 `@Intercepts/@Signature` 匹配时才拦截，否则直接放行
- 本项目 `CreateTimeInterceptor` 拦截 `Executor.update`，反射填充 createTime；
  PageHelper 拦截 `Executor.query`，改写 SQL 加 `LIMIT` 并先执行 count
- 责任链：多个插件按注册顺序嵌套包装

## 8. 本项目 TypeHandler
- `UserTypeTypeHandler` 继承 `BaseTypeHandler<UserType>`，存 int code、读 int→枚举
- 全局注册于 `<typeHandlers>`；MyBatis 根据 javaType 自动匹配
- 相比 MyBatis 内置 `EnumTypeHandler`（按枚举 name 存字符串），code 映射更抗重命名
```

- [ ] **Step 2: AGENTS.md 同步新特性**

在 `AGENTS.md` 更新（目标文件最终形态见下）：

- `Project` 段技术栈补充：HikariCP 5.x、PageHelper 5.3.3、Mockito。
- `Database` 段：schema 含 `user`(含 `user_type`)、`orders`、`account`；三个表都会在每次运行被 DROP 重建 + 种子（zhangsan/lisi、3 条订单、2 个账户）。
- `Architecture` 树补：`entity/Order.java`、`entity/Account.java`、`entity/UserType.java`、`typehandler/`、`interceptor/`、`mapper/OrderMapper.xml`、`mapper/AccountMapper.xml`、`service/impl/AccountServiceImpl.java`、`service/impl/OrderServiceImpl.java`、`docs/mybatis-internals.md`。
- `Conventions` 补：全局 `UserTypeTypeHandler`（枚举 code 映射）；`CreateTimeInterceptor`（insert 自动填充 createTime）；PageHelper + 拦截器已注册于 mybatis-config；`UserMapper.xml` 二级缓存 `<cache/>`；数据源为 HikariCP（`UnpooledDataSourceFactory` 反射 setter 注入）；`selectByConditionOrdered` 的 `${orderBy}` 必须走 service 白名单。
- 新增 `Docs` 段：`docs/mybatis-internals.md`（MyBatis 原理面试讲稿）、`docs/superpowers/specs/`（设计）、`docs/superpowers/plans/`（实施计划）。

- [ ] **Step 3: Commit（可选）**

```bash
git add docs/mybatis-internals.md AGENTS.md
git commit -m "docs: mybatis internals cheatsheet and AGENTS sync"
```

---

### Task 12: 最终验证

**Files:** 无（只运行命令）

- [ ] **Step 1: 干净构建 + 全量测试 + 质量检查**

Run: `mvn clean verify`
Expected: BUILD SUCCESS（checkstyle/spotbugs 的 warning 不阻断）。

- [ ] **Step 2: 运行 demo 冒烟（可选）**

Run: `mvn exec:java`（交互式，需 stdin 输入；`echo 0` 退出）
Expected: 菜单含 0-9 项，可正常退出。

- [ ] **Step 3: 复核硬约束**

- `selectById(1)` 返回 zhangsan（UserMapperTest 断言覆盖）
- 无 Lombok、无 star import、无 `catch(Exception)`
- `yujavalab.db` 未入库（gitignored `*.db`）

---

## 自检记录（spec 覆盖核对）

- 动态 SQL `<where>/<if>/<choose>/<set>` → Task 4
- `#{}` vs `${}` + 白名单 → Task 4 Step 5/6
- 编程式事务 + 回滚 → Task 7
- 二级缓存 `<cache/>` → Task 9
- 多表关联 `<collection>` + OrderMapper → Task 6
- 手写分页 + PageHelper → Task 8 Step 6/7
- 拦截器 CreateTimeInterceptor → Task 8
- HikariCP 连接池 → Task 5
- 自定义 TypeHandler → Task 3
- 原理文档 → Task 11
- 扩展 demo 菜单 → Task 10
- Mockito 单测（TypeHandler + Interceptor）→ Task 3 / Task 8
- 现有测试不破坏 → 各任务 Step 2/9/8 的 `mvn -q test` 全量验证 + Task 12
