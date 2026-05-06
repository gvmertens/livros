# Project Structure

## Repository Layout

```
/
├── backend/                        # Quarkus Maven project
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/library/
│       │   │   ├── identity/       # Auth, users, profiles
│       │   │   ├── catalog/        # Books, authors, publishers
│       │   │   ├── reading/        # Reading records
│       │   │   ├── recommendation/ # Stub only
│       │   │   └── shared/         # Cross-cutting concerns
│       │   └── resources/
│       │       ├── application.properties
│       │       └── db/migration/   # Flyway SQL scripts
│       └── test/
│           └── java/com/library/
└── frontend/                       # React + Vite SPA
    └── src/
        ├── api/                    # Axios API call modules
        ├── auth/                   # AuthContext, ProtectedRoute
        ├── pages/                  # One file per route
        │   └── admin/
        ├── components/             # Shared UI components
        └── types/                  # TypeScript interfaces (mirror API DTOs)
```

## Backend Module Layout (per module)

Each module under `com.library.<module>` follows this four-layer structure:

```
<module>/
├── domain/          # Entities, enums, value objects — no framework deps
├── application/     # Service interfaces + implementations, business logic
├── infrastructure/  # Panache repositories, JWT, password encoding
└── api/             # JAX-RS resources, DTOs, mappers, @Valid annotations
```

**Dependency rule**: `api` → `application` → `domain`. `infrastructure` implements `application` interfaces. No cross-module entity imports — modules communicate only via `EventBus`.

## Shared Module (`com.library.shared`)

| Package | Contents |
|---|---|
| `shared.event` | `EventBus` interface, `InProcessEventBus`, `DomainEvent`, `DomainEventEnvelope` |
| `shared.pagination` | `PageRequest`, `PageResponse<T>` |
| `shared.exception` | `AppException` hierarchy, `GlobalExceptionMapper`, `ErrorResponse` |
| `shared.filter` | `CorrelationIdFilter` |

## Conventions

- **Entities**: extend `PanacheEntityBase`; public fields; `@PrePersist`/`@PreUpdate` set `createdAt`/`updatedAt`
- **UUIDs**: `@GeneratedValue(strategy = GenerationType.UUID)` on all `id` fields
- **DTOs**: Java `record` types; never expose JPA entities in REST responses
- **Exceptions**: throw `AppException` subclasses (`NotFoundException`, `ConflictException`, `ValidationException`, `ForbiddenException`, `UnprocessableEntityException`) — `GlobalExceptionMapper` handles HTTP mapping
- **Events**: always wrap in `DomainEventEnvelope(UUID, eventType, Instant, payload)` before publishing
- **Flyway**: scripts in `src/main/resources/db/migration/`, named `V{n}__{description}.sql`; one table per script; order: users → profiles → authors → publishers → books → readings
- **Pagination**: all list endpoints accept `page` (default 0) and `size` (default 20, max 100) query params; return `PageResponse<T>`
- **Logging**: use `org.jboss.logging.Logger`; include `correlationId` from MDC in error logs
