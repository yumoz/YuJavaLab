# AGENTS.md — java_mybatis

## Project

Single-module Maven project (Java 24, MyBatis 3.5.13, MySQL 8.0.33). A learning demo for MyBatis CRUD with standard layered architecture.

## Entry point

**`com.example.MyBatisDemo`** — the demo runner. Uses `UserService` (not raw mappers).

## Commands

```bash
mvn clean compile                                   # build
mvn exec:java                                       # run (exec-maven-plugin configured)
mvn test                                            # run tests (requires MySQL)
mvn verify                                          # test + checkstyle + spotbugs
mvn checkstyle:check                                # code style only
mvn spotbugs:check                                  # static analysis only
mvn mybatis-generator:generate                      # regenerate entity/mapper from DB
```

## Database

| File | Table fields | Match mapper? |
|---|---|---|
| `db/init.sql` | `username`, `password`, `email`, `create_time` | ✅ |

Single source of truth. Run via Docker:
```bash
docker compose up -d
# or manually:
mysql -u root -p < db/init.sql
```

Edit `src/main/resources/mybatis-config.xml` credentials (default: `mybatis_user` / `MyBatis@123456`).

**Tests require a running MySQL** — start Docker first.

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
    └── SqlSessionFactoryUtil.java # SqlSessionFactory singleton (DCL)
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

`src/test/java/com/example/` — `UserMapperTest` (mapper-level) and `UserServiceTest` (service-level). Run with `mvn test` (MySQL must be up).

## Code quality

Run `mvn verify` to execute Checkstyle (`checkstyle.xml` at root) and SpotBugs (exclusions in `config/spotbugs-exclude.xml`). Failures are warnings only (non-blocking).

## MyBatis Generator

Config: `src/main/resources/generator/generatorConfig.xml`. Run `mvn mybatis-generator:generate` after DB is up. `overwrite=false` — won't overwrite existing hand-written files.
