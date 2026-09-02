一个遵循标准 Java 工程结构的项目模板，演示三层架构 CRUD 的完整实现。

本项目是一个 MyBatis 学习 Demo，通过「Entity → Mapper → Service」三层分层架构，配合 SQLite 嵌入式数据库，展示了 MyBatis 核心特性的完整落地：XML 映射、`resultMap` 下划线转驼峰、`useGeneratedKeys` 主键回填、`foreach` 批量插入等。同时配备 Checkstyle / SpotBugs 代码质量工具链与 GitHub Actions CI。

## 技术栈

- Java 24 · Maven
- ORM: MyBatis 3.5.13
- 数据库: SQLite（无需启动服务，自动创建）
- 日志: SLF4J + Logback
- 测试: JUnit 4.13.2
- 代码质量: Checkstyle + SpotBugs
- CI: GitHub Actions + Dependabot

## 快速开始

### 1. 编译 → 运行

无需启动数据库，首次运行时自动创建 `yujavalab.db` 文件。

```bash
mvn clean compile
mvn exec:java
```

### 2. 测试

```bash
mvn test                        # 运行测试
mvn verify                      # 测试 + Checkstyle + SpotBugs
```

## 命令速查

```bash
mvn compile                     # 编译
mvn exec:java                   # 运行
mvn test                        # 测试
mvn verify                      # 完整验证
mvn checkstyle:check            # 代码规范检查
mvn spotbugs:check              # 静态缺陷检测
mvn mybatis-generator:generate  # 从数据库表生成代码
```

## 项目结构

```
├── pom.xml
├── checkstyle.xml
├── .github/
│   ├── workflows/maven.yml
│   └── dependabot.yml
├── src/main/java/com/example/
│   ├── MyBatisDemo.java
│   ├── entity/User.java
│   ├── mapper/
│   │   ├── UserMapper.java
│   │   └── UserMapper.xml
│   ├── service/
│   │   ├── UserService.java
│   │   └── impl/UserServiceImpl.java
│   └── util/
│       ├── SqlSessionFactoryUtil.java
│       └── DatabaseInit.java
├── src/main/resources/
│   ├── mybatis-config.xml
│   ├── logback.xml
│   ├── db/init.sql
│   └── generator/generatorConfig.xml
├── src/test/java/com/example/
│   ├── mapper/UserMapperTest.java
│   └── service/UserServiceTest.java
└── config/
    └── spotbugs-exclude.xml
```

## 相关文档

- `AGENTS.md` — OpenCode 工作指引

### 技术文档

| 文档 | 内容 |
|---|---|
| [docs/architecture.md](docs/architecture.md) | 三层架构设计、调用链路、SqlSession 生命周期、DCL 单例工厂 |
| [docs/database.md](docs/database.md) | SQLite 表结构、init.sql 初始化流程、字段映射策略 |
| [docs/mybatis-config.md](docs/mybatis-config.md) | mybatis-config.xml、UserMapper.xml 映射详解、Generator 配置 |
| [docs/testing.md](docs/testing.md) | 测试分层体系、初始化机制、数据隔离策略 |
| [docs/code-quality.md](docs/code-quality.md) | Checkstyle 规则清单、SpotBugs 排除策略、非阻塞设计 |
| [docs/ci-cd.md](docs/ci-cd.md) | GitHub Actions CI、CodeQL、Dependabot、MySQL vs SQLite 差异 |
| [docs/dependencies.md](docs/dependencies.md) | 依赖树、版本矩阵、JDK 兼容性、SpotBugs 问题记录 |
