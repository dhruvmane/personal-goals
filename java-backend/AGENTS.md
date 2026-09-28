## Development

Run the API (Flyway migrates the database on first boot):

```
./mvnw spring-boot:run
```

Build a runnable jar:

```
./mvnw clean package
java -jar target/java-backend-0.0.1-SNAPSHOT.jar
```

Run tests (in-memory H2, no database needed):

```
./mvnw test
```

## Conventions

- Layered per feature: `controller -> service -> repository`. Controllers hold no
  business logic, services hold no HTTP concerns.
- Entity fields are never returned directly. Map them to a `*Response` record so
  columns like `password_hash` cannot leak.
- Ownership comes from `@AuthenticationPrincipal`, never from the request body.
- Validation lives on the request records via `@Valid` + Jakarta Bean Validation.
- Flyway owns the schema; Hibernate only runs `ddl-auto=validate`. Never edit an
  applied migration, add a new one.

## Configuration

Secrets come from `.env` (gitignored, copy from `.env.example`). Spring Boot loads it
via `spring.config.import` - do not add a dotenv dependency.

If `JWT_SECRET` is missing or under 32 characters the app fails fast at startup with a
message saying so, rather than booting with a weak or blank signing key.

## Docs

- [Spring Boot reference](https://docs.spring.io/spring-boot/index.html)
- [Spring Security](https://docs.spring.io/spring-security/reference/index.html)
- [Spring Data JPA](https://docs.spring.io/spring-data/jpa/reference/index.html)
- [Flyway](https://documentation.red-gate.com/flyway)
- [Neon Postgres](https://neon.com/docs)
