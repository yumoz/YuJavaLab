# Checklist

- [x] README.md 增加了项目背景简介和技术文档索引章节，所有 docs/ 链接可正常跳转
- [x] docs/architecture.md 描述了三层架构（Entity → Mapper → Service）、MyBatisDemo 菜单循环、SqlSessionFactoryUtil DCL 单例、UserServiceImpl try-with-resources 生命周期
- [x] docs/database.md 描述了 user 表 5 个字段及默认值、init.sql 的 DROP+CREATE+种子数据、DatabaseInit.init() 的 SQL 拆分执行、create_time→createTime 的 resultMap 映射
- [x] docs/mybatis-config.md 覆盖了 mybatis-config.xml（POOLED+JDBC）、UserMapper.xml（namespace、resultMap、useGeneratedKeys、foreach）、generatorConfig.xml
- [x] docs/testing.md 描述了 UserMapperTest（4 用例）和 UserServiceTest（3 用例）、@BeforeClass 初始化、UUID 防重、finally 清理
- [x] docs/code-quality.md 列出了 Checkstyle 15 条规则、SpotBugs 排除 EI_EXPOSE_REP/EI_EXPOSE_REP2、failsOnError=false 非阻塞设计
- [x] docs/ci-cd.md 描述了 Maven CI（JDK 24+MySQL 8.0.33）、CodeQL 安全扫描、Dependabot、CI MySQL 与本地 SQLite 差异
- [x] docs/dependencies.md 包含依赖树、版本矩阵表、JDK 24 vs JDK 25 兼容性、SpotBugs class file major 69 问题记录