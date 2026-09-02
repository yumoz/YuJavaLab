# CI/CD 与持续集成

## 概述

项目通过 GitHub Actions 实现持续集成，配合 Dependabot 自动更新依赖，并启用 CodeQL 进行安全扫描。

## Maven CI 工作流

配置位于 [`.github/workflows/maven.yml`](../.github/workflows/maven.yml)，触发条件为 `main` 分支的 push 和 pull request。

### 构建环境

| 项 | 值 |
|---|---|
| 运行器 | `ubuntu-latest` |
| JDK | 24（Temurin 发行版） |
| 缓存 | Maven 依赖缓存 |

### 服务容器

CI 启动 **MySQL 8.0.33** 服务容器：

```yaml
services:
  mysql:
    image: mysql:8.0.33
    env:
      MYSQL_ROOT_PASSWORD: ${{ secrets.MYSQL_ROOT_PASSWORD }}
      MYSQL_DATABASE: mybatis_demo
    ports:
      - 3306:3306
```

数据库密码通过 GitHub Secrets 注入，健康检查使用 `mysqladmin ping`。

### 步骤流程

1. `actions/checkout@v4` — 检出代码。
2. `actions/setup-java@v4` — 配置 JDK 24 + Temurin。
3. 初始化数据库 — 执行 `mysql < db/init.sql`。
4. `mvn -B verify` — 编译、测试、Checkstyle、SpotBugs。

## CodeQL 安全扫描

配置位于 [`.github/workflows/codeql.yml`](../.github/workflows/codeql.yml)，支持两种语言扫描：

- `java-kotlin`（构建模式 `none`）
- `actions`（工作流本身）

在 push、pull request 和每周定时（周日）触发。

## Dependabot 依赖更新

配置位于 [`.github/dependabot.yml`](../.github/dependabot.yml)，周度检查两个生态：

| 生态 | 目录 | 频率 |
|---|---|---|
| maven | `/` | weekly |
| github-actions | `/` | weekly |

## CI 与本地环境差异

> 重要提示：CI 使用 **MySQL**，本地实际使用 **SQLite**，两者存在差异。

| 维度 | CI（MySQL） | 本地（SQLite） |
|---|---|---|
| 数据库 | MySQL 8.0.33 | SQLite（嵌入式） |
| 服务 | 需要容器服务 | 无需独立服务 |
| 建表语法 | 需兼容 MySQL | SQLite 方言 |
| 连接配置 | 需 credentials | 文件相对路径 |

### 注意事项

- [`db/init.sql`](../src/main/resources/db/init.sql) 目前使用 SQLite 语法（如 `INTEGER PRIMARY KEY AUTOINCREMENT`、`TEXT` 类型），在 MySQL 下可能不完全兼容。若要在 CI 中完整运行，需保持 SQL 在两个方言间可移植。
- 本地应用通过 [`DatabaseInit`](../src/main/java/com/example/util/DatabaseInit.java) 自动初始化 SQLite，而 CI 通过命令行手动导入 MySQL。

## 构建产物

`mvn verify` 通过后会生成 `target/java_mybatis-1.0-SNAPSHOT.jar`，并执行全部测试套件与静态检查。