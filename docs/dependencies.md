# 依赖与环境

## JDK 版本

| 环境 | JDK 版本 | 说明 |
|---|---|---|
| 项目目标（`pom.xml`） | **Java 24** | `maven.compiler.source/target = 24` |
| CI（GitHub Actions） | **JDK 24** | Temurin 发行版 |
| 本地沙箱 | **OpenJDK 25.0.2** | class 文件 major version 69 |

> 沙箱实际运行环境为 JDK 25，高于项目目标 JDK 24，这导致了 SpotBugs 的兼容性问题（见下文）。

## 构建工具

| 工具 | 版本 |
|---|---|
| Maven | 3.9.10 |

## 依赖树

```text
com.example:java_mybatis:jar:1.0-SNAPSHOT
+- org.mybatis:mybatis:jar:3.5.13:compile
+- org.xerial:sqlite-jdbc:jar:3.46.0.0:compile
+- org.slf4j:slf4j-api:jar:2.0.9:compile
+- ch.qos.logback:logback-classic:jar:1.4.14:compile
|  \- ch.qos.logback:logback-core:jar:1.4.14:compile
\- junit:junit:jar:4.13.2:test
   \- org.hamcrest:hamcrest-core:jar:1.3:test
```

## 直接依赖版本矩阵

| 依赖 | GroupId : ArtifactId | 版本 | 作用域 |
|---|---|---|---|
| MyBatis | `org.mybatis:mybatis` | 3.5.13 | compile |
| SQLite JDBC | `org.xerial:sqlite-jdbc` | 3.46.0.0 | compile |
| SLF4J API | `org.slf4j:slf4j-api` | 2.0.9 | compile |
| Logback | `ch.qos.logback:logback-classic` | 1.4.14 | compile |
| JUnit | `junit:junit` | 4.13.2 | test |

### 传递依赖

- `logback-classic` → `logback-core:1.4.14`
- `junit:junit` → `hamcrest-core:1.3`

## 构建插件

| 插件 | 版本 | 用途 |
|---|---|---|
| `exec-maven-plugin` | 3.1.0 | 运行 `com.example.MyBatisDemo` 入口类 |
| `maven-surefire-plugin` | 3.2.5 | 执行单元测试 |
| `maven-checkstyle-plugin` | 3.3.1 | 代码规范检查（validate 阶段） |
| `spotbugs-maven-plugin` | 4.8.6.6 | 静态缺陷检测（verify 阶段） |
| `mybatis-generator-maven-plugin` | 1.4.2 | 从数据库表生成实体/Mapper |

## 已知兼容性问题

### SpotBugs 与 JDK 25 不兼容

在沙箱 JDK 25 环境下运行 `mvn verify`，SpotBugs 在 `spotbugs:spotbugs` 目标失败，报错：

```
Unsupported class file major version 69
java.lang.IllegalArgumentException: Unsupported class file major version 69
```

**原因**：SpotBugs 4.8.6.6 内置的 ASM 版本无法解析 JDK 25（class file major version 69）生成的字节码。

**影响范围**：仅在 JDK 25 环境触发；在 CI 的 JDK 24 环境中不会出现。编译、测试、Checkstyle 均不受影响，可正常通过。

**解决方案**：
1. 使用 JDK 24 运行构建（与 CI 对齐）。
2. 升级 SpotBugs 到支持 JDK 25 的更高版本。
3. 本地跳过 SpotBugs（如 `-Dspotbugs.skip=true`）。

## 网络代理注意事项（沙箱）

沙箱环境出口需通过代理 `127.0.0.1:18080`。Maven 默认不读取环境变量中的代理配置，需显式指定，否则依赖解析报 `Network is unreachable`。

可在项目目录放置 [`Maven 代理配置`](../.maven-proxy-settings.xml)，构建时通过 `-s` 指定：

```bash
mvn -s .maven-proxy-settings.xml -B verify
```