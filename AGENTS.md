# AGENTS.md — java_mybatis

## Project

Single-module Maven project (Java 24, MyBatis 3.5.13, SQLite). A learning demo for MyBatis CRUD with standard layered architecture.

## Entry point

**`com.example.MyBatisDemo`** — the demo runner. Uses `UserService` (not raw mappers).

## Commands

```bash
mvn clean compile                                   # build
mvn exec:java                                       # run (exec-maven-plugin configured)
mvn test                                            # run tests
mvn verify                                          # test + checkstyle + spotbugs
mvn checkstyle:check                                # code style only
mvn spotbugs:check                                  # static analysis only
mvn mybatis-generator:generate                      # regenerate entity/mapper from DB
```

## Database

SQLite — no server needed. The database file `yujavalab.db` is created automatically on first run.

| File | Table fields | Match mapper? |
|---|---|---|
| `db/init.sql` | `username`, `password`, `email`, `create_time` | ✅ |

Schema is initialized via `DatabaseInit.init()` at application startup, which reads `db/init.sql` and executes it against `yujavalab.db`.

## Architecture

```
src/main/java/com/example/
├── MyBatisDemo.java              # entry point
├── entity/User.java              # POJO, handwritten (no Lombok)
├── mapper/
│   ├── UserMapper.java           # Mapper interface
│   └── UserMapper.xml            # SQL mappings
├── service/
│   ├── UserService.java          # service interface
│   └── impl/UserServiceImpl.java # service impl, opens/closes SqlSession
└── util/
    ├── SqlSessionFactoryUtil.java # SqlSessionFactory singleton (DCL)
    └── DatabaseInit.java         # SQLite schema initializer
```

## Conventions

- **No Lombok** — getters/setters are handwritten.
- **Auto-commit** enabled (`openSession(true)`).
- Mapper XML `namespace` must match interface FQN (`com.example.mapper.UserMapper`).
- `create_time` → `createTime` via explicit `<resultMap>` (no `mapUnderscoreToCamelCase`).
- `insertUser` uses `useGeneratedKeys="true" keyProperty="id"`.
- **Logging** via SLF4J + Logback (`logback.xml` in resources).
- **Service layer** owns session lifecycle (try-with-resources).

## Tests

`src/test/java/com/example/` — `UserMapperTest` (mapper-level) and `UserServiceTest` (service-level). Tests call `DatabaseInit.init()` in `@BeforeClass` to ensure schema exists.

## Code quality

Run `mvn verify` to execute Checkstyle (`checkstyle.xml` at root) and SpotBugs (exclusions in `config/spotbugs-exclude.xml`). Failures are warnings only (non-blocking).

## MyBatis Generator

Config: `src/main/resources/generator/generatorConfig.xml`. Run `mvn mybatis-generator:generate` after DB is created. `overwrite=false` — won't overwrite existing hand-written files.
