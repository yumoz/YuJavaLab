# 补充技术文档 Spec

## Why

项目目前仅有 `README.md`（快速上手）和 `AGENTS.md`（AI 工作指引），缺少系统性的技术文档来阐述架构设计、数据流、配置策略、测试体系、代码质量工具链、CI/CD 流水线以及环境依赖分析。补充技术文档可帮助新成员和后续维护者快速理解项目全貌与设计决策。

## What Changes

- 扩充 `README.md`，在现有"快速开始"基础上增加技术文档章节入口和项目背景介绍
- 新增 `docs/architecture.md`：三层架构设计、调用链路、SqlSession 生命周期、单例工厂（DCL）设计
- 新增 `docs/database.md`：SQLite 表结构、字段约定、`init.sql` 初始化流程、`create_time` → `createTime` 映射策略
- 新增 `docs/mybatis-config.md`：`mybatis-config.xml` 配置解析、`UserMapper.xml` 映射详解（resultMap、useGeneratedKeys、foreach 批量插入）、Generator 配置
- 新增 `docs/testing.md`：测试分层策略（Mapper 级 / Service 级）、`@BeforeClass` 初始化、UUID 防重、测试清理机制
- 新增 `docs/code-quality.md`：Checkstyle 规则清单、SpotBugs 排除策略、`failsOnError=false` 非阻塞设计
- 新增 `docs/ci-cd.md`：GitHub Actions（Maven CI + CodeQL）、Dependabot 配置、CI 中 MySQL 与本地 SQLite 的差异说明
- 新增 `docs/dependencies.md`：依赖树、版本矩阵、JDK 版本兼容性（JDK 24 目标 vs JDK 25 沙箱）、SpotBugs ASM 兼容问题

## Impact

- 受影响文档：`README.md`（扩充）
- 受影响代码：无（纯文档变更）
- 新增文件：`docs/` 目录下 7 个 Markdown 文档

## ADDED Requirements

### Requirement: 架构文档
系统 SHALL 提供 `docs/architecture.md`，描述三层架构（Entity → Mapper → Service）、入口类 `MyBatisDemo` 的菜单循环、`SqlSessionFactoryUtil` 双重检查锁定单例模式、`UserServiceImpl` 中 try-with-resources 管理的 SqlSession 生命周期。

#### Scenario: 新成员阅读架构文档
- **WHEN** 新成员打开 `docs/architecture.md`
- **THEN** 能理解从用户输入到数据库操作的完整调用链路，以及 SqlSession 的创建与关闭时机

### Requirement: 数据库文档
系统 SHALL 提供 `docs/database.md`，描述 `user` 表结构（id/username/password/email/create_time）、`init.sql` 的 DROP + CREATE + 种子数据流程、`DatabaseInit.init()` 的 SQL 拆分执行逻辑、以及 `<resultMap>` 中 `create_time` → `createTime` 的显式映射（未启用 `mapUnderscoreToCamelCase`）。

#### Scenario: 查看数据库设计
- **WHEN** 开发者打开 `docs/database.md`
- **THEN** 能了解表字段定义、默认值、种子数据、以及 MyBatis 字段映射策略

### Requirement: MyBatis 配置文档
系统 SHALL 提供 `docs/mybatis-config.md`，覆盖 `mybatis-config.xml`（POOLED 数据源、JDBC 事务管理）、`UserMapper.xml`（namespace 约定、resultMap、useGeneratedKeys 主键回填、foreach 批量插入）、以及 `generatorConfig.xml` 代码生成器配置。

#### Scenario: 理解 Mapper 映射
- **WHEN** 开发者打开 `docs/mybatis-config.md`
- **THEN** 能理解每个 SQL 语句的映射方式、主键回填机制、批量插入的 foreach 语法

### Requirement: 测试文档
系统 SHALL 提供 `docs/testing.md`，描述两层测试体系（`UserMapperTest` 4 个用例 + `UserServiceTest` 3 个用例）、`@BeforeClass` 调用 `DatabaseInit.init()`、UUID 后缀防数据冲突、finally 块清理测试数据。

#### Scenario: 了解测试策略
- **WHEN** 开发者打开 `docs/testing.md`
- **THEN** 能理解测试分层、初始化机制、数据隔离策略

### Requirement: 代码质量文档
系统 SHALL 提供 `docs/code-quality.md`，列出 Checkstyle 启用的 15 条规则、SpotBugs 排除的 `EI_EXPOSE_REP` / `EI_EXPOSE_REP2` 模式、以及 `failsOnError=false` 非阻塞设计理念。

#### Scenario: 查看代码规范
- **WHEN** 开发者打开 `docs/code-quality.md`
- **THEN** 能了解项目遵循的代码规范和静态分析配置

### Requirement: CI/CD 文档
系统 SHALL 提供 `docs/ci-cd.md`，描述 Maven CI 工作流（JDK 24 + MySQL 8.0.33 服务容器）、CodeQL 安全扫描、Dependabot 周度更新、以及 CI 使用 MySQL 与本地 SQLite 的差异和注意事项。

#### Scenario: 理解 CI 流水线
- **WHEN** 开发者打开 `docs/ci-cd.md`
- **THEN** 能了解 CI 触发条件、构建步骤、安全扫描覆盖范围

### Requirement: 依赖环境文档
系统 SHALL 提供 `docs/dependencies.md`，包含完整依赖树、版本矩阵表、JDK 24（项目目标）vs JDK 25（沙箱环境）兼容性说明、SpotBugs 4.8.6.6 无法解析 class file major 69 的问题记录。

#### Scenario: 排查环境问题
- **WHEN** 开发者遇到 SpotBugs 构建失败
- **THEN** 能在 `docs/dependencies.md` 中找到原因说明和解决方案

### Requirement: README 扩充
系统 SHALL 在 `README.md` 中增加技术文档索引章节，链接到 `docs/` 下的各文档，并补充项目背景与设计理念简介。

#### Scenario: 从 README 导航到技术文档
- **WHEN** 用户打开 `README.md`
- **THEN** 能通过文档索引快速跳转到对应的技术文档
