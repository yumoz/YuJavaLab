# AGENTS.md — java_mybatis

## Project

Single-module Maven project (Java 24, MyBatis 3.5.13, SQLite, HikariCP 5.x, PageHelper 5.3.3, JUnit 4, Mockito 5). A learning demo covering MyBatis interview topics: dynamic SQL, `#{}` vs `${}`, programmatic transactions, first/second-level cache, one-to-many mapping, pagination (manual + PageHelper), plugin/interceptor, custom TypeHandler, connection pool.

> **Runtime note:** pom targets Java 24, but the dev box runs JDK 25. Keep `mockito-core` recent (≥5.23) — older versions bundle a byte-buddy that can't instrument JDK 25 class files.

## Entry point

**`com.example.MyBatisDemo`** — the demo runner. Uses `UserService` (not raw mappers). `mvn exec:java` opens an interactive console menu that reads from stdin — it blocks waiting for input in a non-interactive shell.

## Commands

```bash
mvn clean compile                                   # build
mvn exec:java                                       # run (interactive menu, blocks on stdin)
mvn test                                            # all tests
mvn test -Dtest=UserMapperTest                      # single test class
mvn verify                                          # test + checkstyle + spotbugs
mvn checkstyle:check                                # code style only
mvn spotbugs:check                                  # static analysis only
mvn mybatis-generator:generate                      # regenerate entity/mapper from DB
```

## Database

SQLite — no server needed. **DB URL is configurable** via the system property `app.db.url` (default `jdbc:sqlite:yujavalab.db`). `SqlSessionFactoryUtil` injects it into the `${app.db.url}` placeholder in `mybatis-config.xml` at build time, so it must stay consistent with `DatabaseInit.DEFAULT_DB_URL`.

**DB is reset on every run/test.** `DatabaseInit.init()` executes `src/main/resources/db/init.sql` (classpath `/db/init.sql`), which `DROP TABLE IF EXISTS user`, recreates it, and seeds `zhangsan`/`lisi`. Do not expect state to persist between runs.

**Test isolation:** surefire pins the test JVM to `jdbc:sqlite:target/test.db` (via `systemPropertyVariables` in `pom.xml`), so tests never touch the dev `yujavalab.db`; `mvn clean` removes it.

Schema (from `init.sql`): `user` (`id`, `username`, `password`, `email`, `user_type`, `create_time`), `orders` (`id`, `user_id`, `order_no`, `amount`, `create_time`, seeded 3 rows), `account` (`id`, `account_no`, `name`, `balance`, seeded 2 rows). `data/users.csv` is a sample input for the demo's batch-import menu option. Tests depend on seed balances (1000/500); `AccountServiceTest` rebuilds the DB in `@Before` per case and is **not** order-sensitive anymore.

## Architecture

```
src/main/java/com/example/
├── MyBatisDemo.java              # entry point
├── entity/                       # User, UserType(enum), UserQuery, Order, Account(BigDecimal balance)
├── mapper/
│   ├── UserMapper.java/xml       # CRUD + dynamic SQL + pagination + user-with-orders
│   ├── OrderMapper.java/xml      # orders CRUD (one-to-many child table)
│   └── AccountMapper.java/xml    # debit/credit atomic balance updates (transaction demo)
├── service/
│   ├── UserService.java/impl     # session lifecycle via SqlSessionTemplate
│   ├── OrderService.java/impl
│   └── AccountService.java/impl  # transfer(): programmatic transaction, BizException
├── exception/BizException.java   # business exception (insufficient balance, bad params, ...)
├── interceptor/CreateTimeInterceptor.java  # auto-fills createTime on insert
├── typehandler/UserTypeTypeHandler.java    # enum <-> int code
├── datasource/HikariDataSourceFactory.java # MyBatis DataSourceFactory for HikariCP
└── util/
    ├── SqlSessionFactoryUtil.java # SqlSessionFactory singleton (DCL, injects app.db.url)
    ├── SqlSessionTemplate.java    # session lifecycle template (auto-commit / programmatic tx)
    └── DatabaseInit.java         # SQLite schema initializer
```

Config lives in `src/main/resources/`: `mybatis-config.xml` (HikariCP datasource via `com.example.datasource.HikariDataSourceFactory`, `logImpl=SLF4J`, registers all 3 mappers, global TypeHandler + PageHelper + CreateTimeInterceptor plugins), `logback.xml` (`org.apache.ibatis` at DEBUG to see dynamic SQL / pagination rewrite / cache hits), `db/init.sql`, `generator/generatorConfig.xml`. Docs: `docs/mybatis-internals.md` (MyBatis internals interview notes), `docs/spring-integration-guide.md` (how to evolve to Spring-managed), `docs/superpowers/specs|plans/` (design + implementation plan).

## Conventions

- **No Lombok** — getters/setters are handwritten.
- **Session lifecycle via `SqlSessionTemplate`** (`com.example.util.SqlSessionTemplate`): `execute(...)` for auto-commit (`openSession(true)`), `executeInTransaction(...)` for programmatic tx (`openSession(false)` + commit/rollback). Exception: none — `AccountServiceImpl.transfer()` uses `SqlSessionTemplate.executeInTransaction`.
- Mapper XML `namespace` must match interface FQN (`com.example.mapper.UserMapper`).
- `create_time` → `createTime` via explicit `<resultMap>` (no `mapUnderscoreToCamelCase`).
- `insertUser`/`insertBatch`/`insertOrder` use `useGeneratedKeys="true" keyProperty="id"`.
- **Logging** via SLF4J + Logback (`com.example` at DEBUG, `org.apache.ibatis` at DEBUG, `org.mybatis` at INFO).
- `UserType` enum ↔ DB int via global `UserTypeTypeHandler`; add a `user_type` column reference to any new user SQL.
- `CreateTimeInterceptor` auto-fills `createTime` on any insert (no need to set it manually).
- `UserMapper.xml` enables second-level cache (`<cache readOnly="true"/>`); entity must stay non-Serializable-friendly, do NOT add `readOnly="false"`. Cache behavior is covered by `CacheTest`.
- `${orderBy}` in `selectByConditionOrdered` **must** be validated by the service whitelist (`UserServiceImpl.ORDER_BY_WHITELIST`) — never pass user input into `${}` directly.
- `Account.balance` is `BigDecimal` — compare with `compareTo`, never `equals` (scale differs).
- `AccountMapper.debit`/`credit` are **atomic SQL updates** (`balance = balance ± #{amount} WHERE id = #{id} AND balance >= #{amount}`) — the read-then-write pattern is forbidden for balance changes; `transfer()` checks the affected row count for concurrency protection.
- Business rule violations throw `com.example.exception.BizException` (extends `RuntimeException`).

## Tests

`src/test/java/com/example/` — `UserMapperTest`, `OrderMapperTest`, `CacheTest` (mapper-level, second-level cache hit/evict) and `UserServiceTest`, `AccountServiceTest` (service-level; `AccountServiceTest` rebuilds the DB in `@Before` per case — **not order-sensitive**), plus Mockito unit tests `UserTypeTypeHandlerTest`/`CreateTimeInterceptorTest`. All DB-backed tests call `DatabaseInit.init()` and **depend on the seed data** (`selectById(1)` must return `zhangsan`). Editing `init.sql`'s seed rows or the `DROP`/`INSERT` statements will break them.

## Code quality

`mvn verify` runs Checkstyle (`checkstyle.xml` at root) and SpotBugs (exclusions in `config/spotbugs-exclude.xml`). Both are configured `failsOnError=false` (checkstyle severity `warning`) — **failures are warnings only, non-blocking**.

## CI

`.github/workflows/maven.yml` — JDK 24 (temurin, Maven cache) + `mvn -B verify` on ubuntu-latest. No database service needed (SQLite is embedded; tests use `target/test.db`). Triggers on `main` push/PR. This replaced the old stale MySQL-based workflow.

## MyBatis Generator

Config: `src/main/resources/generator/generatorConfig.xml`. Run `mvn mybatis-generator:generate` after DB is created. `overwrite=false` — won't overwrite existing hand-written files.
