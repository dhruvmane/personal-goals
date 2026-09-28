# java-backend

Spring Boot 3 API for the Personal Goals Tracker. JWT auth, Neon (Postgres) via Flyway,
and CRUD for goals and users.

## Stack

- Java 21 (builds on newer JDKs)
- Spring Boot 3.5, Spring Web, Spring Data JPA, Spring Security, Bean Validation
- PostgreSQL via Neon, schema managed by Flyway
- JWT (jjwt) bearer tokens, HS256
- Maven wrapper (`./mvnw`) - no local Maven install needed

## Setup

1. Create a project at [console.neon.tech](https://console.neon.tech) and copy the
   connection string. Keep `sslmode=require` in the URL.

2. Create your local env file:

   ```bash
   cp .env.example .env
   ```

   Fill in `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`, and `JWT_SECRET`.
   Generate a secret with:

   ```bash
   node -e "console.log(require('crypto').randomBytes(48).toString('base64url'))"
   ```

   `.env` is gitignored. Never commit it.

3. Run it. Flyway creates the `users` and `goals` tables on first boot:

   ```bash
   ./mvnw spring-boot:run
   ```

   API is on `http://localhost:8080`.

`.env` is loaded by Spring Boot itself via `spring.config.import` - no extra plugin
needed. Environment variables override it, so it works fine in Docker/CI.

### Notes on Neon

Use the **pooled** connection string (the hostname ending in `-pooler`) for the running
server - it goes through Neon's connection pooler and survives more traffic. Both the
pooled and direct strings work for migrations.

Serverless Postgres suspends when idle, so the first request after a pause may take a
second. The pool size is capped at 10 by default (`DB_POOL_SIZE`) - raise it only if you
are on a paid plan, since pooled connections are limited.

## Tests

```bash
./mvnw test
```

Tests run against an in-memory H2 database in PostgreSQL compatibility mode, so no Neon
connection is needed. They cover register/login, the goal CRUD cycle, ownership scoping
between users, the admin gate, and validation failures.

## API

Base URL `http://localhost:8080`. All requests and responses are JSON.
Send authenticated requests as `Authorization: Bearer <token>`.

### Public

| Method | Path              | Purpose                        |
| ------ | ----------------- | ------------------------------ |
| GET    | `/api/health`     | Liveness check                 |
| POST   | `/api/auth/register` | Create account, returns token |
| POST   | `/api/auth/login`    | Exchange credentials for token |

### Authenticated

| Method | Path                | Purpose                        |
| ------ | ------------------- | ------------------------------ |
| GET    | `/api/auth/me`      | Current user                   |
| GET    | `/api/goals`        | List your goals, newest first  |
| POST   | `/api/goals`        | Create a goal                  |
| GET    | `/api/goals/{id}`   | Fetch one goal                 |
| PUT    | `/api/goals/{id}`   | Update a goal                  |
| DELETE | `/api/goals/{id}`   | Delete a goal                  |
| GET    | `/api/goals/stats`  | Goal counts per status         |

### Admin only (`ROLE_ADMIN`)

| Method | Path              | Purpose                     |
| ------ | ----------------- | --------------------------- |
| GET    | `/api/users`      | List all users              |
| GET    | `/api/users/{id}` | Fetch a user                |
| PUT    | `/api/users/{id}` | Update email/name/password/role |
| DELETE | `/api/users/{id}` | Delete a user               |

Goal routes are always scoped to the token's user. Requesting another user's goal id
returns `404`, not `403`, so the API does not confirm that the id exists.

`GET /api/goals/stats` returns counts per status:

```json
{ "PENDING": 2, "IN_PROGRESS": 1, "COMPLETED": 5, "ARCHIVED": 0 }
```

### Goal shape

```json
{
  "id": "3f1c1e6a-6f1e-4a3d-9a4b-1c2d3e4f5a6b",
  "userId": "8a2b1c9d-...",
  "title": "Run a 10k",
  "description": "Easy pace, no hills",
  "status": "IN_PROGRESS",
  "progress": 40,
  "targetDate": "2026-12-01",
  "createdAt": "2026-09-28T12:00:00Z",
  "updatedAt": "2026-09-28T12:30:00Z"
}
```

`status` is one of `PENDING`, `IN_PROGRESS`, `COMPLETED`, `ARCHIVED`.
`progress` is `0`-`100`.

## Route snippets

### GET - read

```java
@GetMapping
public List<GoalResponse> list(@AuthenticationPrincipal User current) {
    return goalService.list(current.getId());
}
```

`@AuthenticationPrincipal` injects the `User` the JWT resolved to. Ownership comes from
the token, never the request body, so one user cannot read another user's goals.

### POST - create

```java
@PostMapping
public ResponseEntity<GoalResponse> create(
        @AuthenticationPrincipal User current,
        @Valid @RequestBody GoalCreateRequest request) {
    GoalResponse created = goalService.create(current.getId(), request);
    return ResponseEntity
            .created(URI.create("/api/goals/" + created.id()))
            .body(created);
}
```

`@Valid` triggers the Bean Validation annotations on the request record. Returns `201`
with a `Location` header pointing at the new goal. Only `title` is required; `status`
defaults to `PENDING` and `progress` to `0`.

### PUT - update

```java
@PutMapping("/{id}")
public GoalResponse update(
        @AuthenticationPrincipal User current,
        @PathVariable UUID id,
        @Valid @RequestBody GoalUpdateRequest request) {
    return goalService.update(current.getId(), id, request);
}
```

Omitted fields are left untouched, which is what you want for a form that PATCHes one
field. Note this makes `PUT` behave as a partial update rather than a full replace - if
you need strict replace semantics later, that is a deliberate change to make.

## Try it

```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"dhruv@example.com","password":"s3cret-pass","displayName":"Dhruv"}'

# Log in, copy the token out of the response
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"dhruv@example.com","password":"s3cret-pass"}'

export TOKEN="<token>"

# Create
curl -X POST http://localhost:8080/api/goals \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"title":"Run a 10k","description":"Easy pace","progress":40,"targetDate":"2026-12-01"}'

# Read
curl http://localhost:8080/api/goals -H "Authorization: Bearer $TOKEN"

# Update
curl -X PUT http://localhost:8080/api/goals/<id> \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"status":"COMPLETED","progress":100}'
```

## Becoming an admin

New accounts are `USER`. To promote one, run against the Neon SQL editor:

```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'dhruv@example.com';
```

Sign in again afterwards - the role is baked into the token at login, so the existing
token will not pick up the change until you log in fresh.

## Errors

Failures return a consistent body:

```json
{
  "timestamp": "2026-09-28T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/auth/register",
  "fieldErrors": { "password": "size must be between 8 and 72" }
}
```

`400` validation, `401` missing or invalid token, `403` wrong role, `404` missing or
not-yours, `409` duplicate email.

## Layout

```
src/main/java/com/personalgoals/
  PersonalGoalsApplication.java
  auth/       register + login, JWT issue/verify, auth filter
  config/     SecurityConfig - routes, CORS, password hashing
  goal/       Goal CRUD (entity, repo, service, controller, DTOs)
  user/       User entity, repo, service, admin controller
  health/     /api/health
  common/     error handling, ApiError, NotFound, Conflict
src/main/resources/
  application.yml
  db/migration/V1__create_users_and_goals.sql
```

Layered per feature: controller -> service -> repository. Controllers do no business
logic and services do no HTTP work.

## Frontend integration

`CORS_ALLOWED_ORIGINS` defaults to `http://localhost:4321` (Astro's dev port). Comma
separate for more. Credentials are allowed, so you can also switch to an httpOnly cookie
later without changing the CORS config.

## Things to change before production

- Email verification and a password reset flow - neither exists yet.
- Refresh tokens. Access tokens currently last `JWT_EXPIRES_IN` (default 12h) with no
  refresh, so users re-login when it expires.
- Rate limiting on `/api/auth/*` to slow credential stuffing.
- `Goal` holds a plain `userId` column rather than a `@ManyToOne`. The FK constraint
  enforces integrity and there are no lazy-loading pitfalls, but a JPA association reads
  better if you start navigating the graph.
- Editing an applied Flyway migration breaks its checksum. Add a new `V2__...` file
  instead - this is only safe right now because V1 has never been deployed.
