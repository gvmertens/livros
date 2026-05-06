# Tech Stack

## Backend

- **Language**: Java 21
- **Framework**: Quarkus 3.8.4
- **Build**: Maven (`backend/pom.xml`)
- **Persistence**: Hibernate ORM with Panache + PostgreSQL (JDBC)
- **Migrations**: Flyway (auto-runs on startup via `quarkus.flyway.migrate-at-start=true`)
- **Auth**: SmallRye JWT (RS256); keys at `META-INF/resources/privateKey.pem` / `publicKey.pem`
- **REST**: RESTEasy Reactive + Jackson
- **DI**: Quarkus ArC (CDI)
- **Logging**: JBoss Logging + MDC for correlation IDs

## Frontend

- **Language**: TypeScript
- **Framework**: React + Vite
- **HTTP**: Axios (with JWT interceptor)
- **Routing**: React Router v6
- **Server state**: TanStack React Query
- **Auth state**: React Context + `useReducer`

## Testing

- **Unit/Integration**: JUnit 5 via `quarkus-junit5`
- **REST integration**: REST Assured + `quarkus-test-security`
- **Database (tests)**: Testcontainers (PostgreSQL 1.19.7)
- **Property-based**: jqwik 1.8.4
- **Coverage**: JaCoCo — minimum 80% line coverage on `*Service` and `*Domain*` classes

## Common Commands

All commands run from the `backend/` directory.

```bash
# Run in dev mode (hot reload)
./mvnw quarkus:dev

# Run tests (single pass, no watch)
./mvnw test

# Build production JAR
./mvnw package

# Build + verify coverage thresholds
./mvnw verify

# Run a specific test class
./mvnw test -Dtest=MyServiceTest
```
