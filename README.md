# Java MyBatis Demo

一个MyBatis入门学习项目，演示MyBatis的基本CRUD操作。

## 项目功能

- 根据ID查询用户
- 查询所有用户
- 新增用户

## 技术栈

- Java 24
- MyBatis 3.5.13
- MySQL 8.0.33

## 编译运行

### 1. 创建数据库

```bash
mysql -u root -p < src/db.sql
```

或执行 `src/db.sql` 中的SQL语句创建数据库和表。

### 2. 配置数据库连接

编辑 `src/main/resources/mybatis-config.xml`，修改数据库用户名和密码：

```xml
<property name="username" value="your_username"/>
<property name="password" value="your_password"/>
```

### 3. 编译

```bash
mvn clean compile
```

### 4. 运行

```bash
mvn exec:java -Dexec.mainClass="com.example.MyBatisDemo"
```

或在IDE中直接运行 `MyBatisDemo.java`。

## 项目结构

```
src/
├── main/
│   ├── java/com/example/
│   │   ├── Main.java              # 入口类
│   │   ├── MyBatisDemo.java       # MyBatis演示类
│   │   ├── entity/User.java       # 用户实体类
│   │   └── mapper/UserMapper.java # Mapper接口
│   └── resources/
│       ├── mybatis-config.xml     # MyBatis全局配置
│       └── mapper/UserMapper.xml  # Mapper映射文件
└── db.sql                         # 数据库建表脚本
```
