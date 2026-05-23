# Java MyBatis Demo

一个 MyBatis 入门学习项目，遵循标准 Java 工程结构，演示 MyBatis 的基本 CRUD 操作。

## 技术栈

- Java 24
- MyBatis 3.5.13
- MySQL 8.0.33
- SLF4J + Logback
- JUnit 4.13.2
- Maven Checkstyle + SpotBugs

## 快速开始

### 1. 启动数据库（二选一）

**方式 A：Docker Compose（推荐）**
```bash
docker compose up -d
```

**方式 B：手动执行 SQL**
```bash
mysql -u root -p < db/init.sql
```

### 2. 配置数据库连接

编辑 `src/main/resources/mybatis-config.xml`，确认用户名密码正确（默认：`mybatis_user` / `MyBatis@123456`）。

### 3. 编译

```bash
mvn clean compile
```

### 4. 运行

```bash
mvn exec:java
```

或在 IDE 中直接运行 `MyBatisDemo.java`。

### 5. 运行测试

```bash
mvn test
```

### 6. 完整验证（含代码质量检查）

```bash
mvn verify
```

### 7. 代码质量单项检查

```bash
mvn checkstyle:check    # 代码规范检查
mvn spotbugs:check      # 静态缺陷检测
```

### 8. MyBatis Generator

需要先启动数据库并执行 `db/init.sql`：

```bash
mvn mybatis-generator:generate
```

## 项目结构

```
├── pom.xml
├── docker-compose.yml
├── checkstyle.xml
├── db/
│   └── init.sql                     # 数据库初始化脚本
├── src/
│   ├── main/
│   │   ├── java/com/example/
│   │   │   ├── MyBatisDemo.java     # 入口类
│   │   │   ├── entity/User.java     # 用户实体类
│   │   │   ├── mapper/
│   │   │   │   ├── UserMapper.java  # Mapper 接口
│   │   │   │   └── UserMapper.xml   # SQL 映射
│   │   │   ├── service/
│   │   │   │   ├── UserService.java
│   │   │   │   └── impl/UserServiceImpl.java
│   │   │   └── util/
│   │   │       └── SqlSessionFactoryUtil.java
│   │   └── resources/
│   │       ├── mybatis-config.xml
│   │       ├── logback.xml
│   │       ├── mapper/UserMapper.xml
│   │       └── generator/
│   │           └── generatorConfig.xml
│   └── test/java/com/example/
│       ├── mapper/UserMapperTest.java
│       └── service/UserServiceTest.java
└── config/
    └── spotbugs-exclude.xml
```
