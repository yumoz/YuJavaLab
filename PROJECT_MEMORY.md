# 项目持久记忆 Project Memory

> 本文件由 dsh-memoir 插件维护：记录本项目历次会话的工作归纳、经验教训与行动指南，
> 作为未来 AGENTS 接手本项目时的行动指南。会话开始时自动注入 system prompt。

## 工作记录 Work Log

- [2026-08-21 19:45] [工作记录] 输出 java_mybatis 项目改进建议清单 — 已通读全部核心代码并给出 13 条改进建议，分三级优先级：(1) 立即修——转账金额缺正数校验（负数可反向造钱）、转账并发丢更新（updateBalance 整体写回）、DatabaseInit 写死相对路径 jdbc:sqlite:yujavalab.db 且测试共享库文件导致 AccountServiceTest 依赖 @FixMethodOrder(NAME_ASCENDING)、CI maven.yml 仍用 MySQL 已 stale、README 结构停留在早期形态；(2) 教学加分——原子扣减 UPDATE + 锁演示、二级缓存已配但无命中/失效测试、Service 层 try-with-resources 样板重复可抽 SqlSessionTemplate；(3) 方向可选——Spring 集成、统一异常体系、BigDecimal 金额、密码哈希、logback 中 org.mybatis 为 INFO 看不到 SQL（建议调 DEBUG 或加 logImpl=SLF4J）。
- [2026-08-21 19:59] [工作记录] java_mybatis 12 项改进全部落地并回归通过 — 完成 java_mybatis 全部 12 项改进并 mvn verify 全绿（26 测试 0 失败、Checkstyle+SpotBugs 0 缺陷）：(1) 转账修复——金额正数校验（负数可反向造钱）、AccountMapper 改原子扣减 debit/credit（UPDATE ... balance=balance-? WHERE id=? AND balance>=?）+ 影响行数判定防并发超扣；(2) Account.balance double→BigDecimal（含实体/接口/Demo/测试，断言用 compareTo 规避 scale 差异）；(3) 测试隔离——app.db.url 系统属性注入 mybatis-config 的 ${app.db.url} 占位符，surefire 固定测试用 target/test.db；(4) AccountServiceTest 每用例 @Before 重建库，彻底去除 @FixMethodOrder 顺序敏感；(5) 新增 CacheTest 验证二级缓存命中(assertSame)/insert 失效；(6) 抽 SqlSessionTemplate 统一 session 生命周期（execute 自动提交 / executeInTransaction 程序化事务）；(7) 新增 BizException 统一业务异常；(8) logImpl=SLF4J + org.apache.ibatis DEBUG 打开 SQL 日志；(9) CI maven.yml 去掉 stale MySQL 改 SQLite+JDK24；(10) README 重写 + AGENTS.md 同步；(11) docs/spring-integration-guide.md 交付 Spring 演进指南（Spring 集成以文档形式落地，避免改变裸 MyBatis 教学定位）；(12) 密码哈希以 README 注明教学演示处理。

## 经验教训 Lessons Learned

- [2026-08-21 19:59] [经验教训] JDK25+Mockito self-attach 失败用 subclass maker 解决 — JDK 25 + Mockito 5.x（5.23.0）在 Windows 上报 "Could not self-attach to current VM using external process"，inline mock maker 无法初始化（不是版本旧的问题）。解法：在 src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker 写入 mock-maker-subclass 切换 subclass maker；但 subclass maker 无法 mock final 类（org.apache.ibatis.mapping.MappedStatement 是 final），测试中若拦截器不读该参数可传 null 占位（CreateTimeInterceptor 只用 args[1]）。PowerShell 管道 `mvn ... 2>&1 | Select-Object` 会让 mvn 的退出码误报为 1（实际 BUILD SUCCESS），确认退出码应改用 `mvn -B verify *> log; $LASTEXITCODE`。

## 行动指南 Action Guide

- [2026-08-21 19:45] [行动指南] java_mybatis 改进待用户确认是否实施 — 已向用户提议从「立即做」清单动手：转账金额正数校验+测试、CI 改回 SQLite、打开 SQL 日志、README 更新、测试库隔离（内存/临时文件库+系统属性配置 URL）。用户尚未确认实施范围，等待下一步指示；若用户选择实施，优先做这几项并补对应测试。
- [2026-08-21 19:59] [行动指南] 改进后待办：本地体验 + 可选 Spring 演进 — 后续可做：(1) 用户本地跑 mvn exec:java 体验负数转账被拒 + 观察 SQL 日志；(2) 若想演进 Spring，按 docs/spring-integration-guide.md 迁移（@MapperScan/@Transactional，Mapper 与 XML 不用改）；(3) 未来若需 mock final 类/方法，需升级 byte-buddy 或恢复 inline maker 并解决 attach。
