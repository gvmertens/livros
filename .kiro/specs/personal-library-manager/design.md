# Design Document — Personal Library Manager

## Overview

The Personal Library Manager is a full-stack web application built as a **modular monolith** in its first iteration. The backend is a Quarkus application (Java 21, Maven) backed by PostgreSQL, using Hibernate ORM with Panache for persistence, Flyway for schema migrations, and SmallRye JWT (RS256) for stateless authentication. The frontend is a React + TypeScript + Vite single-page application communicating with the backend over REST.

The system is divided into four backend modules — **identity**, **catalog**, **reading**, and **recommendation** (stub) — plus a **shared** module for cross-cutting concerns. Each module owns its own domain entities, repositories, services, REST resources, DTOs, and mappers. Modules communicate exclusively through domain events published to an internal `EventBus`, keeping compile-time coupling between modules to zero.

The architecture is designed so that any module can be extracted into an independent microservice later without changing its internal logic: the event bus interface is swappable for a real broker (Kafka, SQS), and each module's public API surface is defined through explicit interfaces rather than shared entity classes.

---

## Architecture

### High-Level Structure

```
┌─────────────────────────────────────────────────────────────┐
│                        React SPA (Vite)                      │
│  LoginPage  RegisterPage  BookListPage  MyReadingsPage  ...  │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTPS / REST (Axios + JWT Bearer)
┌────────────────────────▼────────────────────────────────────┐
│                   Quarkus HTTP Layer                         │
│  GlobalExceptionMapper   CorrelationIdFilter   /q/health     │
├──────────────┬───────────────┬──────────────┬───────────────┤
│  identity    │    catalog    │   reading    │recommendation │
│  module      │    module     │   module     │  stub module  │
├──────────────┴───────────────┴──────────────┴───────────────┤
│                    shared module                             │
│         EventBus  •  DomainEvent  •  PageResponse           │
├─────────────────────────────────────────────────────────────┤
│              Hibernate ORM / Panache  +  PostgreSQL          │
└─────────────────────────────────────────────────────────────┘
```

### Module Boundaries

```
com.library
├── identity/
│   ├── domain/          User, Profile, Role (enum)
│   ├── application/     UserService, AuthService, ProfileService
│   ├── infrastructure/  UserRepository, ProfileRepository, JwtIssuer, BcryptPasswordEncoder
│   └── api/             AuthResource, UserResource, RegisterRequest, LoginRequest, UserResponse, ProfileResponse, ProfileUpdateRequest
├── catalog/
│   ├── domain/          Book, Author, Publisher
│   ├── application/     BookService, AuthorService, PublisherService
│   ├── infrastructure/  BookRepository, AuthorRepository, PublisherRepository
│   └── api/             BookResource, AuthorResource, PublisherResource, + DTOs/Mappers
├── reading/
│   ├── domain/          Reading, ReadingStatus (enum)
│   ├── application/     ReadingService
│   ├── infrastructure/  ReadingRepository
│   └── api/             ReadingResource, + DTOs/Mappers
├── recommendation/
│   ├── domain/          RecommendationRequest, RecommendationResponse (contracts only)
│   └── api/             RecommendationResource (stub — returns empty list)
└── shared/
    ├── event/           EventBus (interface), InProcessEventBus, DomainEvent, DomainEventEnvelope
    ├── pagination/      PageRequest, PageResponse<T>
    ├── exception/       AppException, ErrorCode, GlobalExceptionMapper
    └── filter/          CorrelationIdFilter
```

### Clean Architecture Layers (per module)

| Layer | Responsibility | Allowed dependencies |
|---|---|---|
| `domain` | Entities, value objects, domain rules | None (pure Java) |
| `application` | Use-case services, business logic orchestration | `domain`, `shared` |
| `infrastructure` | Panache repositories, JWT, password hashing | `domain`, `application` |
| `api` | JAX-RS resources, DTOs, mappers, validation | `application`, `domain`, `shared` |

---

## Components and Interfaces

### Shared Module

#### EventBus

```java
public interface EventBus {
    void publish(DomainEventEnvelope event);
}
```

`InProcessEventBus` is the default implementation. It dispatches events synchronously in-process using CDI `@ApplicationScoped` observers. The interface is the only coupling point; swapping to Kafka requires only a new implementation bound via `@Alternative`.

```java
@ApplicationScoped
public class InProcessEventBus implements EventBus {
    @Inject Event<DomainEventEnvelope> cdiEvent;

    @Override
    public void publish(DomainEventEnvelope envelope) {
        cdiEvent.fire(envelope);
    }
}
```

#### DomainEventEnvelope

```java
public record DomainEventEnvelope(
    UUID eventId,
    String eventType,
    Instant occurredAt,
    Object payload   // serialized to JSON on publish
) {}
```

#### PageResponse\<T\>

```java
public record PageResponse<T>(
    List<T> content,
    long totalElements,
    int totalPages,
    int page,
    int size
) {}
```

#### GlobalExceptionMapper

A JAX-RS `ExceptionMapper<Throwable>` that catches all exceptions and returns a consistent JSON error body:

```json
{ "status": 400, "message": "...", "timestamp": "ISO-8601" }
```

Specific `AppException` subclasses map to their HTTP status codes. Unhandled exceptions map to 500 and log at ERROR with the correlation ID.

#### CorrelationIdFilter

A `ContainerRequestFilter` that reads `X-Correlation-ID` from the incoming request (or generates a UUID if absent), stores it in MDC, and writes it back in the response header.

---

### Identity Module

#### AuthService (application layer)

```java
public interface AuthService {
    UserResponse register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
}
```

Responsibilities: validate uniqueness of email, hash password with bcrypt (cost 12), persist `User`, create empty `Profile`, publish `user.created` event, issue JWT on login.

#### UserService (application layer)

```java
public interface UserService {
    UserResponse getCurrentUser(UUID userId);
    void updateRole(UUID targetUserId, Role newRole);
}
```

#### ProfileService (application layer)

```java
public interface ProfileService {
    ProfileResponse getProfile(UUID requestingUserId, UUID profileOwnerId);
    ProfileResponse updateProfile(UUID userId, ProfileUpdateRequest request);
}
```

#### JwtIssuer (infrastructure layer)

Wraps SmallRye JWT `JWTParser` / `Jwt.claims()` builder. Signs tokens with RS256 using the private key from `mp.jwt.verify.privatekey.location` config. Token claims: `sub` (userId), `email`, `groups` (role).

#### UserRepository / ProfileRepository (infrastructure layer)

Panache repository interfaces:

```java
public interface UserRepository {
    Optional<User> findByEmail(String email);
    User findByIdOrThrow(UUID id);
    void persist(User user);
}

public interface ProfileRepository {
    Optional<Profile> findByUserId(UUID userId);
    void persist(Profile profile);
}
```

#### REST Resources (api layer)

- `AuthResource` — `POST /api/v1/auth/register`, `POST /api/v1/auth/login` (no auth)
- `UserResource` — `GET /api/v1/users/me`, `PUT /api/v1/users/{id}/role` (JWT required)
- `ProfileResource` — `GET /api/v1/users/me/profile`, `PUT /api/v1/users/me/profile` (JWT required)

---

### Catalog Module

#### AuthorService / PublisherService / BookService (application layer)

```java
public interface AuthorService {
    AuthorResponse create(AuthorRequest request);
    AuthorResponse update(UUID id, AuthorRequest request);
    void delete(UUID id);
    PageResponse<AuthorResponse> list(PageRequest page);
    AuthorResponse getById(UUID id);
}
```

`BookService` additionally validates that `authorId` and `publisherId` exist before persisting, and publishes `book.created`, `book.updated`, `book.deleted` events.

#### BookRepository (infrastructure layer)

```java
public interface BookRepository {
    Optional<Book> findByIsbn(String isbn);
    Book findByIdOrThrow(UUID id);
    PageResponse<BookResponse> search(String query, PageRequest page);
    void persist(Book book);
    void delete(Book book);
}
```

The `search` method uses a JPQL `LOWER(b.title) LIKE :q OR LOWER(a.name) LIKE :q` query with a join on Author.

#### REST Resources (api layer)

- `AuthorResource` — full CRUD at `/api/v1/authors`
- `PublisherResource` — full CRUD at `/api/v1/publishers`
- `BookResource` — full CRUD at `/api/v1/books`

---

### Reading Module

#### ReadingService (application layer)

```java
public interface ReadingService {
    ReadingResponse create(UUID userId, CreateReadingRequest request);
    ReadingResponse update(UUID requestingUserId, UUID readingId, UpdateReadingRequest request, boolean isAdmin);
    void delete(UUID requestingUserId, UUID readingId);
    PageResponse<ReadingResponse> listForUser(UUID userId, ReadingStatus statusFilter, PageRequest page);
    ReadingResponse getById(UUID requestingUserId, UUID readingId);
}
```

Ownership checks: if `requestingUserId != reading.userId` and `!isAdmin`, throw `ForbiddenException`.

Business rules enforced in service:
- Duplicate reading (userId + bookId) → 409
- Rating outside 0.0–10.0 → 400
- Status FINISHED without startedAt → 400

Events published: `reading.created`, `rating.updated` (when rating changes), `review.submitted` (when review changes).

#### ReadingRepository (infrastructure layer)

```java
public interface ReadingRepository {
    Optional<Reading> findByUserIdAndBookId(UUID userId, UUID bookId);
    Reading findByIdOrThrow(UUID id);
    PageResponse<ReadingResponse> findByUserId(UUID userId, ReadingStatus status, PageRequest page);
    void persist(Reading reading);
    void delete(Reading reading);
}
```

#### REST Resource (api layer)

- `ReadingResource` — full CRUD at `/api/v1/readings`

---

### Recommendation Module (Stub)

```java
@Path("/api/v1/recommendations")
@Authenticated
public class RecommendationResource {
    @GET
    public RecommendationResponse getRecommendations() {
        return new RecommendationResponse(List.of());
    }
}
```

`RecommendationRequest` and `RecommendationResponse` are defined in `recommendation/domain` as the shared contract for future integration.

---

## Data Models

### Database Schema

#### Table: `users`

```sql
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(255)        NOT NULL,
    email         VARCHAR(255)        NOT NULL UNIQUE,
    password_hash VARCHAR(255)        NOT NULL,
    role          VARCHAR(10)         NOT NULL DEFAULT 'USER'
                      CHECK (role IN ('USER', 'ADMIN')),
    created_at    TIMESTAMPTZ         NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ         NOT NULL DEFAULT now()
);
CREATE INDEX idx_users_email ON users(email);
```

#### Table: `profiles`

```sql
CREATE TABLE profiles (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID        NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    display_name     VARCHAR(100),
    bio              VARCHAR(1000),
    favorite_genres  TEXT[],
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_profiles_user_id ON profiles(user_id);
```

#### Table: `authors`

```sql
CREATE TABLE authors (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
```

#### Table: `publishers`

```sql
CREATE TABLE publishers (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
```

#### Table: `books`

```sql
CREATE TABLE books (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    isbn         VARCHAR(13)  NOT NULL UNIQUE,
    title        VARCHAR(500) NOT NULL,
    author_id    UUID         NOT NULL REFERENCES authors(id),
    publisher_id UUID         NOT NULL REFERENCES publishers(id),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_books_isbn      ON books(isbn);
CREATE INDEX idx_books_author_id ON books(author_id);
CREATE INDEX idx_books_title     ON books USING gin(to_tsvector('english', title));
```

#### Table: `readings`

```sql
CREATE TABLE readings (
    id          UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID           NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    book_id     UUID           NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    status      VARCHAR(20)    NOT NULL DEFAULT 'WANT_TO_READ'
                    CHECK (status IN ('WANT_TO_READ','READING','FINISHED','ABANDONED')),
    rating      NUMERIC(4,2)   CHECK (rating >= 0.0 AND rating <= 10.0),
    review      TEXT,
    started_at  TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uq_readings_user_book UNIQUE (user_id, book_id),
    CONSTRAINT chk_finished_after_started
        CHECK (finished_at IS NULL OR started_at IS NULL OR finished_at >= started_at)
);
CREATE INDEX idx_readings_user_id ON readings(user_id);
CREATE INDEX idx_readings_status  ON readings(user_id, status);
```

### Flyway Migration Order

| Version | File | Description |
|---|---|---|
| V1 | `V1__create_users.sql` | `users` table |
| V2 | `V2__create_profiles.sql` | `profiles` table |
| V3 | `V3__create_authors.sql` | `authors` table |
| V4 | `V4__create_publishers.sql` | `publishers` table |
| V5 | `V5__create_books.sql` | `books` table |
| V6 | `V6__create_readings.sql` | `readings` table |

All scripts live in `src/main/resources/db/migration/`. Flyway runs automatically on application startup via `quarkus.flyway.migrate-at-start=true`.

---

### JPA Entity Design

Each entity extends `PanacheEntityBase` and uses `@Entity`, `@Table`. UUIDs are generated with `@GeneratedValue` using `GenerationType.UUID`. Timestamps use `@PrePersist` / `@PreUpdate` lifecycle hooks to set `createdAt` / `updatedAt`.

Example (Book):

```java
@Entity
@Table(name = "books")
public class Book extends PanacheEntityBase {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(nullable = false, unique = true, length = 13)
    public String isbn;

    @Column(nullable = false, length = 500)
    public String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    public Author author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publisher_id", nullable = false)
    public Publisher publisher;

    public Instant createdAt;
    public Instant updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate  void onUpdate() { updatedAt = Instant.now(); }
}
```

---

## JWT Authentication Flow

```
Client                          AuthResource              JwtIssuer           SmallRye JWT
  │                                  │                        │                    │
  │── POST /auth/login ─────────────►│                        │                    │
  │   {email, password}              │                        │                    │
  │                                  │── verify password ────►│                    │
  │                                  │◄─ ok ─────────────────│                    │
  │                                  │── buildToken(claims) ─►│                    │
  │                                  │                        │── sign RS256 ─────►│
  │                                  │                        │◄─ signed JWT ──────│
  │                                  │◄─ JWT string ─────────│                    │
  │◄─ 200 {token, expiresIn} ────────│                        │                    │
  │                                  │                        │                    │
  │── GET /api/v1/books ────────────►│ (any protected resource)                   │
  │   Authorization: Bearer <JWT>    │                        │                    │
  │                                  │◄── SmallRye validates JWT automatically ───│
  │                                  │    (checks sig, exp, issuer)               │
  │◄─ 200 {books...} ────────────────│                        │                    │
```

**JWT Claims:**

| Claim | Value |
|---|---|
| `sub` | User UUID (string) |
| `email` | User email |
| `groups` | `["USER"]` or `["ADMIN"]` (SmallRye role claim) |
| `iss` | `personal-library-manager` |
| `exp` | `iat + 3600` |

**Configuration (`application.properties`):**

```properties
mp.jwt.verify.publickey.location=META-INF/resources/publicKey.pem
smallrye.jwt.sign.key.location=META-INF/resources/privateKey.pem
mp.jwt.verify.issuer=personal-library-manager
```

Role-based access is enforced with `@RolesAllowed("ADMIN")` / `@RolesAllowed({"USER","ADMIN"})` on JAX-RS resource methods.

---

## Event Bus Design

### In-Process Implementation (Phase 1)

The `InProcessEventBus` uses CDI `Event<DomainEventEnvelope>` to fire events synchronously within the same JVM. Observers are CDI beans annotated with `@Observes`.

```java
// Publisher side (e.g., BookService)
eventBus.publish(new DomainEventEnvelope(
    UUID.randomUUID(),
    "book.created",
    Instant.now(),
    new BookCreatedPayload(book.id, book.isbn, book.title, book.author.id, book.publisher.id)
));

// Observer side (e.g., future RecommendationService)
public void onBookCreated(@Observes DomainEventEnvelope envelope) {
    if ("book.created".equals(envelope.eventType())) { ... }
}
```

### Broker Swap Strategy (Phase 3)

To swap to Kafka/SQS, provide a new `@Alternative @Priority(1)` implementation of `EventBus` that serializes the envelope to JSON and publishes to the broker topic. No changes to any service class are needed.

```
EventBus (interface)
  ├── InProcessEventBus   @ApplicationScoped (default)
  └── KafkaEventBus       @Alternative @Priority(1) (Phase 3)
```

### Event Topics

| Topic | Events |
|---|---|
| `user-events` | `user.created` |
| `book-events` | `book.created`, `book.updated`, `book.deleted` |
| `reading-events` | `reading.created`, `rating.updated`, `review.submitted` |

---

## Error Handling

### AppException Hierarchy

```
AppException (RuntimeException)
├── NotFoundException          → 404
├── ConflictException          → 409
├── ValidationException        → 400
├── ForbiddenException         → 403
└── UnprocessableEntityException → 422
```

### GlobalExceptionMapper

```java
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {
    @Override
    public Response toResponse(Throwable ex) {
        if (ex instanceof AppException ae) {
            return errorResponse(ae.getStatus(), ae.getMessage());
        }
        // ConstraintViolationException from Jakarta Validation
        if (ex instanceof ConstraintViolationException cve) {
            String msg = cve.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(joining(", "));
            return errorResponse(400, msg);
        }
        log.error("Unhandled exception [correlationId={}]", MDC.get("correlationId"), ex);
        return errorResponse(500, "Internal server error");
    }

    private Response errorResponse(int status, String message) {
        return Response.status(status)
            .entity(new ErrorResponse(status, message, Instant.now()))
            .build();
    }
}
```

### ErrorResponse DTO

```java
public record ErrorResponse(int status, String message, Instant timestamp) {}
```

---

## Frontend Architecture

### Project Structure

```
frontend/
├── src/
│   ├── api/
│   │   ├── client.ts          # Axios instance + interceptors
│   │   ├── auth.ts            # register, login API calls
│   │   ├── books.ts           # book CRUD API calls
│   │   ├── authors.ts
│   │   ├── publishers.ts
│   │   └── readings.ts
│   ├── auth/
│   │   ├── AuthContext.tsx    # JWT storage, user state, login/logout
│   │   └── ProtectedRoute.tsx # Route guard (auth + optional role check)
│   ├── pages/
│   │   ├── LoginPage.tsx
│   │   ├── RegisterPage.tsx
│   │   ├── BookListPage.tsx
│   │   ├── BookDetailPage.tsx
│   │   ├── MyReadingsPage.tsx
│   │   ├── ReadingDetailPage.tsx
│   │   ├── ProfilePage.tsx
│   │   └── admin/
│   │       ├── AdminBookFormPage.tsx
│   │       ├── AdminAuthorListPage.tsx
│   │       ├── AdminAuthorFormPage.tsx
│   │       ├── AdminPublisherListPage.tsx
│   │       └── AdminPublisherFormPage.tsx
│   ├── components/
│   │   ├── Navbar.tsx
│   │   ├── Pagination.tsx
│   │   ├── BookCard.tsx
│   │   ├── ReadingStatusBadge.tsx
│   │   └── StarRating.tsx
│   ├── types/
│   │   └── index.ts           # TypeScript interfaces matching API DTOs
│   ├── App.tsx                # Router setup
│   └── main.tsx
├── index.html
├── vite.config.ts
└── tsconfig.json
```

### Axios Client Setup

```typescript
// src/api/client.ts
import axios from 'axios';

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1',
  headers: { 'Content-Type': 'application/json' },
});

// Request interceptor — attach JWT
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('jwt');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Response interceptor — handle 401 globally
client.interceptors.response.use(
  (res) => res,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('jwt');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default client;
```

### AuthContext

```typescript
// src/auth/AuthContext.tsx
interface AuthState {
  user: { id: string; email: string; role: 'USER' | 'ADMIN' } | null;
  login: (token: string) => void;
  logout: () => void;
}
```

On `login(token)`: decode JWT payload (base64), store token in `localStorage`, set user state. On `logout()`: clear storage, redirect to `/login`.

### ProtectedRoute

```typescript
// src/auth/ProtectedRoute.tsx
interface Props {
  requiredRole?: 'ADMIN';
}

// Renders <Outlet /> if authenticated (and role matches).
// Redirects to /login if not authenticated.
// Redirects to /books if authenticated but wrong role.
```

### React Router Setup

```typescript
// src/App.tsx
<BrowserRouter>
  <Routes>
    <Route path="/login"    element={<LoginPage />} />
    <Route path="/register" element={<RegisterPage />} />

    <Route element={<ProtectedRoute />}>
      <Route path="/books"           element={<BookListPage />} />
      <Route path="/books/:id"       element={<BookDetailPage />} />
      <Route path="/readings"        element={<MyReadingsPage />} />
      <Route path="/readings/:id"    element={<ReadingDetailPage />} />
      <Route path="/profile"         element={<ProfilePage />} />
    </Route>

    <Route element={<ProtectedRoute requiredRole="ADMIN" />}>
      <Route path="/admin/books/new"              element={<AdminBookFormPage />} />
      <Route path="/admin/books/:id/edit"         element={<AdminBookFormPage />} />
      <Route path="/admin/authors"                element={<AdminAuthorListPage />} />
      <Route path="/admin/authors/new"            element={<AdminAuthorFormPage />} />
      <Route path="/admin/authors/:id/edit"       element={<AdminAuthorFormPage />} />
      <Route path="/admin/publishers"             element={<AdminPublisherListPage />} />
      <Route path="/admin/publishers/new"         element={<AdminPublisherFormPage />} />
      <Route path="/admin/publishers/:id/edit"    element={<AdminPublisherFormPage />} />
    </Route>
  </Routes>
</BrowserRouter>
```

### State Management

No global state library (Redux, Zustand) is introduced in Phase 1. State is managed with:

- `AuthContext` (React Context + `useReducer`) for authentication state
- `useState` + `useEffect` for page-level data fetching
- React Query (`@tanstack/react-query`) for server state caching, loading/error states, and cache invalidation on mutations

This keeps the dependency footprint minimal while providing solid async state handling.

### Frontend Component Design per Screen

#### BookListPage
- Fetches `GET /books?page=&size=&search=`
- State: `query` (search string), `page`, books data
- Components: `Navbar`, search `<input>`, `BookCard` list, `Pagination`

#### BookDetailPage
- Fetches `GET /books/:id`
- If user has no reading for this book: shows "Add to Library" button → `POST /readings`
- If reading exists: shows current status, rating, review with edit form

#### MyReadingsPage
- Fetches `GET /readings?status=&page=&size=`
- State: `statusFilter`, `page`, readings data
- Components: status filter tabs, reading list rows with `ReadingStatusBadge`

#### ReadingDetailPage
- Fetches `GET /readings/:id`
- Form for updating status, rating (0–10 slider), review (textarea)
- On submit: `PUT /readings/:id`

#### AdminBookFormPage (create + edit)
- On mount: fetches authors list and publishers list for `<select>` dropdowns
- Form fields: isbn, title, authorId, publisherId
- On submit: `POST /books` or `PUT /books/:id`

---

## Correctness Properties


*A property is a characteristic or behavior that should hold true across all valid executions of a system — essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Valid registration always produces a USER-role account

*For any* valid registration input (non-empty name, well-formed email, password ≥ 8 characters), calling `AuthService.register()` SHALL return a user with role USER, a non-null UUID, and the same email as the input.

**Validates: Requirements 1.1**

---

### Property 2: Invalid registration input is rejected

*For any* registration input where the email is malformed OR the password is shorter than 8 characters, `AuthService.register()` SHALL throw a `ValidationException` (400).

**Validates: Requirements 1.3, 1.4**

---

### Property 3: Duplicate email registration is rejected

*For any* email address, if a user with that email already exists, a second registration attempt with the same email SHALL throw a `ConflictException` (409).

**Validates: Requirements 1.2**

---

### Property 4: Password is stored as bcrypt hash

*For any* successful registration with plaintext password P, the stored `passwordHash` SHALL satisfy `BCrypt.checkpw(P, passwordHash) == true` and SHALL NOT equal P.

**Validates: Requirements 1.5, NFR 1.2**

---

### Property 5: Successful registration publishes user.created event

*For any* valid registration input, exactly one `user.created` event SHALL be published to the EventBus, containing the new user's id, email, and createdAt timestamp.

**Validates: Requirements 1.6, 9.2**

---

### Property 6: Correct credentials always yield a valid JWT

*For any* registered user, calling `AuthService.login()` with the correct password SHALL return a JWT whose decoded claims contain `sub` = userId, `email` = user email, `groups` containing the user's role, and `exp` = `iat + 3600`.

**Validates: Requirements 2.1**

---

### Property 7: Incorrect credentials always yield 401

*For any* login attempt where the email does not exist OR the password does not match the stored hash, `AuthService.login()` SHALL throw an `UnauthorizedException` (401).

**Validates: Requirements 2.2, 2.3**

---

### Property 8: Non-admin users are forbidden from catalog mutations

*For any* create, update, or delete request on Books, Authors, or Publishers made with a USER-role JWT, the system SHALL return a 403 Forbidden response.

**Validates: Requirements 3.1, 3.2, 3.3**

---

### Property 9: Profile is auto-created on user registration

*For any* successful user registration, a Profile SHALL exist for that user with null or empty `displayName`, `bio`, and `favoriteGenres`.

**Validates: Requirements 4.1**

---

### Property 10: Profile update is a round-trip

*For any* valid profile update payload (displayName ≤ 100 chars, bio ≤ 1000 chars), calling `ProfileService.updateProfile()` and then `ProfileService.getProfile()` SHALL return a profile whose fields match the update payload exactly.

**Validates: Requirements 4.2, 4.3**

---

### Property 11: Profile field length violations are rejected

*For any* profile update where `displayName` exceeds 100 characters OR `bio` exceeds 1000 characters, `ProfileService.updateProfile()` SHALL throw a `ValidationException` (400).

**Validates: Requirements 4.5, 4.6**

---

### Property 12: Cross-user profile access is forbidden

*For any* two distinct users A and B, user A requesting or updating user B's profile SHALL receive a `ForbiddenException` (403).

**Validates: Requirements 4.4**

---

### Property 13: Author/Publisher with blank name is rejected

*For any* create or update request for an Author or Publisher where the name is blank or composed entirely of whitespace, the service SHALL throw a `ValidationException` (400).

**Validates: Requirements 5.7, 6.7**

---

### Property 14: Author with associated books cannot be deleted

*For any* Author that has at least one associated Book, a delete request SHALL throw a `ConflictException` (409). Conversely, *for any* Author with no associated Books, deletion SHALL succeed and a subsequent lookup SHALL return 404.

**Validates: Requirements 5.3, 5.4, 6.3, 6.4**

---

### Property 15: Book creation is a round-trip

*For any* valid book payload (unique ISBN, non-blank title, existing authorId, existing publisherId), creating a book and then retrieving it by ID SHALL return a book whose isbn, title, author.id, and publisher.id match the creation input.

**Validates: Requirements 7.1**

---

### Property 16: Duplicate ISBN is rejected

*For any* ISBN, creating two books with the same ISBN SHALL cause the second creation to throw a `ConflictException` (409).

**Validates: Requirements 7.2**

---

### Property 17: Book search returns only matching results

*For any* search query string Q and any set of books in the catalog, all books returned by the search SHALL have a title or author name that contains Q (case-insensitive), and no book whose title and author name both do not contain Q SHALL appear in the results.

**Validates: Requirements 7.12, 11.4**

---

### Property 18: Catalog events are published on book mutations

*For any* successful book creation, update, or deletion, exactly one event of the corresponding type (`book.created`, `book.updated`, `book.deleted`) SHALL be published to the EventBus, with a payload containing at minimum the bookId.

**Validates: Requirements 7.9, 7.10, 7.11, 9.3, 9.4, 9.5**

---

### Property 19: New reading always starts with WANT_TO_READ

*For any* authenticated user and any bookId not already in that user's reading list, creating a reading SHALL return a Reading with `status = WANT_TO_READ` and a non-null id.

**Validates: Requirements 8.1**

---

### Property 20: Duplicate reading is rejected

*For any* (userId, bookId) pair where a Reading already exists, a second create request for the same pair SHALL throw a `ConflictException` (409).

**Validates: Requirements 8.2**

---

### Property 21: Reading update is a round-trip

*For any* valid reading update payload (status ∈ {WANT_TO_READ, READING, FINISHED, ABANDONED}, rating ∈ [0.0, 10.0] or null, any review text), updating a reading and then retrieving it SHALL return a Reading whose fields match the update payload.

**Validates: Requirements 8.3**

---

### Property 22: Out-of-range rating is rejected

*For any* rating value R where R < 0.0 or R > 10.0, a reading update with that rating SHALL throw a `ValidationException` (400).

**Validates: Requirements 8.9**

---

### Property 23: Cross-user reading mutation is forbidden

*For any* two distinct users A and B, user A attempting to update or delete a Reading owned by user B (without ADMIN role) SHALL receive a `ForbiddenException` (403).

**Validates: Requirements 8.5, 8.8**

---

### Property 24: Reading events are published on reading mutations

*For any* successful reading creation, rating update, or review update, exactly one event of the corresponding type (`reading.created`, `rating.updated`, `review.submitted`) SHALL be published to the EventBus, with a payload containing at minimum the readingId, userId, and bookId.

**Validates: Requirements 8.11, 8.12, 8.13, 9.6, 9.7, 9.8**

---

### Property 25: Paginated responses always include pagination metadata

*For any* call to a list endpoint, the response SHALL contain `totalElements`, `totalPages`, `page`, and `size` fields, and `size` SHALL be ≤ 100.

**Validates: Requirements 11.1, 11.3**

---

### Property 26: Oversized page size is rejected

*For any* list request where the `size` parameter is greater than 100, the system SHALL return a 400 Bad Request response.

**Validates: Requirements 11.2**

---

### Property 27: All error responses have a consistent structure

*For any* request that results in an error (4xx or 5xx), the response body SHALL be a JSON object containing `status` (integer), `message` (string), and `timestamp` (ISO-8601 string) fields.

**Validates: Requirements 12.1**

---

## Error Handling

### Error Response Contract

All error responses use `ErrorResponse`:

```json
{
  "status": 409,
  "message": "Email already registered",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### Exception-to-HTTP Mapping

| Exception | HTTP Status | Trigger |
|---|---|---|
| `NotFoundException` | 404 | Entity not found by ID |
| `ConflictException` | 409 | Duplicate email, ISBN, reading; delete with dependents |
| `ValidationException` | 400 | Business rule violation (password length, rating range, etc.) |
| `ForbiddenException` | 403 | Cross-user access, insufficient role |
| `UnprocessableEntityException` | 422 | FK reference not found (authorId, publisherId) |
| `ConstraintViolationException` | 400 | Jakarta Validation (`@NotBlank`, `@Email`, `@Size`) |
| `UnauthorizedException` | 401 | Bad credentials |
| `Throwable` (catch-all) | 500 | Unhandled — logged at ERROR with correlationId |

### Correlation ID

`CorrelationIdFilter` (JAX-RS `ContainerRequestFilter`) runs on every request:
1. Reads `X-Correlation-ID` header; generates `UUID.randomUUID()` if absent.
2. Stores in `MDC.put("correlationId", id)`.
3. Adds `X-Correlation-ID` to the response via `ContainerResponseFilter`.

All log statements automatically include the correlation ID via the MDC pattern in `application.properties`:

```properties
quarkus.log.console.format=%d{HH:mm:ss} %-5p [%c{2.}] [%X{correlationId}] %s%e%n
```

---

## Testing Strategy

### Backend

**Unit tests** (JUnit 5 + Mockito):
- All service classes (`AuthService`, `BookService`, `ReadingService`, etc.)
- Business rule validations (duplicate checks, rating range, FINISHED without startedAt)
- Mapper correctness (entity → DTO field mapping)
- EventBus payload construction

**Property-based tests** (jqwik):
- Each correctness property above is implemented as a `@Property` test with `tries = 100`
- Generators: `@ForAll @StringLength(min=8) String password`, `@ForAll @Email String email`, `@ForAll @DoubleRange(min=-100, max=200) double rating`, etc.
- Tag format: `// Feature: personal-library-manager, Property N: <property text>`

**Integration tests** (Quarkus `@QuarkusTest` + REST Assured + Testcontainers PostgreSQL):
- All REST endpoints (happy path + error cases)
- JWT issuance and validation end-to-end
- Flyway migration correctness
- Event publishing verified with a test `EventBus` observer

**Coverage target**: ≥ 80% line coverage on all service and domain classes (enforced via JaCoCo Maven plugin).

### Frontend

**Unit tests** (Vitest + React Testing Library):
- `AuthContext` login/logout state transitions
- `ProtectedRoute` redirect behavior
- Form validation feedback (empty fields, invalid email)
- `Pagination` component page navigation

**Integration tests** (Vitest + MSW for API mocking):
- `BookListPage` search and pagination
- `MyReadingsPage` status filter
- `AdminBookFormPage` create/edit form submission


---

## Frontend Architecture — Phase 4 Enhancements (F1–F4)

### Updated Frontend Project Structure

The following files are added to the existing structure for the F1–F4 features:

```
frontend/
└── src/
    ├── api/
    │   └── ... (existing)
    ├── auth/
    │   └── ... (existing)
    ├── components/
    │   ├── Navbar.tsx              # Updated: includes LanguageSwitcher
    │   ├── BookCard.tsx            # Updated: displays cover thumbnail with fallback
    │   ├── LanguageSwitcher.tsx    # NEW (F1): inline "PT | EN" toggle
    │   └── BookCoverFetcher.tsx    # NEW (F3): cover search + preview component
    ├── i18n/
    │   ├── index.ts                # NEW (F1): i18next configuration
    │   └── locales/
    │       ├── pt-BR.json          # NEW (F1): all UI strings in Brazilian Portuguese
    │       └── en-US.json          # NEW (F1): all UI strings in American English
    ├── pages/
    │   ├── LoginPage.tsx           # Updated (F4): split-screen layout
    │   └── ... (existing)
    ├── services/
    │   └── bookCoverService.ts     # NEW (F3): Open Library cover fetch service
    ├── utils/
    │   └── getLanguageInstruction.ts  # NEW (F2): AI prompt language instruction utility
    ├── assets/
    │   └── images/
    │       └── login-bg.jpg        # NEW (F4): local fallback library background image
    └── types/
        └── index.ts                # Updated: Book/BookRequest/BookResponse include optional coverUrl
```

---

### F1 — i18n Architecture

#### Overview

Internationalization is implemented using `react-i18next` and `i18next`. The application supports two locales: `pt-BR` (Brazilian Portuguese, default) and `en-US` (American English). Language preference is persisted in `localStorage` and restored on the next session.

#### i18next Configuration (`src/i18n/index.ts`)

```typescript
import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';
import ptBR from './locales/pt-BR.json';
import enUS from './locales/en-US.json';

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: {
      'pt-BR': { translation: ptBR },
      'en-US': { translation: enUS },
    },
    fallbackLng: 'pt-BR',
    supportedLngs: ['pt-BR', 'en-US'],
    detection: {
      order: ['localStorage', 'navigator'],
      lookupLocalStorage: 'i18n_language',
      caches: ['localStorage'],
    },
    interpolation: {
      escapeValue: false, // React already escapes values
    },
  });

export default i18n;
```

The `i18next-browser-languagedetector` plugin handles reading and writing the `i18n_language` key in `localStorage` automatically. The `fallbackLng: 'pt-BR'` ensures that if no preference is stored and the browser language is not `en-US`, the interface defaults to Portuguese.

#### Locale File Structure

Both locale files share the same key structure. Keys are organized by feature area:

```json
// src/i18n/locales/pt-BR.json (excerpt)
{
  "nav": {
    "books": "Livros",
    "myReadings": "Minhas Leituras",
    "recommendations": "Recomendações",
    "profile": "Perfil",
    "logout": "Sair"
  },
  "auth": {
    "login": "Entrar",
    "register": "Cadastrar",
    "email": "E-mail",
    "password": "Senha",
    "name": "Nome"
  },
  "books": {
    "search": "Buscar livros...",
    "addToLibrary": "Adicionar à Biblioteca",
    "isbn": "ISBN",
    "title": "Título",
    "author": "Autor",
    "publisher": "Editora"
  },
  "cover": {
    "notFound": "Capa não encontrada",
    "loading": "Buscando capa...",
    "searchByTitle": "Buscar por título/autor"
  },
  "login": {
    "quote": "\"Um leitor vive mil vidas antes de morrer. Quem nunca lê, vive apenas uma.\" — George R.R. Martin"
  },
  "recommendations": {
    "generatedIn": "Gerado em PT"
  }
}
```

```json
// src/i18n/locales/en-US.json (excerpt)
{
  "nav": {
    "books": "Books",
    "myReadings": "My Readings",
    "recommendations": "Recommendations",
    "profile": "Profile",
    "logout": "Logout"
  },
  "auth": {
    "login": "Sign In",
    "register": "Register",
    "email": "Email",
    "password": "Password",
    "name": "Name"
  },
  "books": {
    "search": "Search books...",
    "addToLibrary": "Add to Library",
    "isbn": "ISBN",
    "title": "Title",
    "author": "Author",
    "publisher": "Publisher"
  },
  "cover": {
    "notFound": "Cover not found",
    "loading": "Searching for cover...",
    "searchByTitle": "Search by title/author"
  },
  "login": {
    "quote": "\"A reader lives a thousand lives before he dies. The man who never reads lives only one.\" — George R.R. Martin"
  },
  "recommendations": {
    "generatedIn": "Generated in EN"
  }
}
```

#### LanguageSwitcher Component (`src/components/LanguageSwitcher.tsx`)

```typescript
import { useTranslation } from 'react-i18next';

const LOCALES = ['pt-BR', 'en-US'] as const;
type Locale = typeof LOCALES[number];

const LABELS: Record<Locale, string> = {
  'pt-BR': 'PT',
  'en-US': 'EN',
};

export function LanguageSwitcher() {
  const { i18n } = useTranslation();
  const active = i18n.language as Locale;

  return (
    <div className="language-switcher" aria-label="Language selector">
      {LOCALES.map((locale, idx) => (
        <span key={locale}>
          {idx > 0 && <span className="separator" aria-hidden="true"> | </span>}
          <button
            className={`lang-btn ${active === locale ? 'active' : ''}`}
            style={active === locale ? { color: '#E07020', fontWeight: 600 } : {}}
            onClick={() => i18n.changeLanguage(locale)}
            aria-pressed={active === locale}
            aria-label={`Switch to ${LABELS[locale]}`}
          >
            {LABELS[locale]}
          </button>
        </span>
      ))}
    </div>
  );
}
```

**Design decisions:**
- No dropdown — direct toggle between two languages on click.
- Active language rendered in brand orange `#E07020` with `font-weight: 600`.
- `aria-pressed` attribute ensures screen readers announce the active state.
- Positioned in `Navbar.tsx` on the right side, before the avatar/profile element.

#### Navbar Integration

```typescript
// src/components/Navbar.tsx (updated right section)
<nav className="navbar">
  <div className="navbar-left">...</div>
  <div className="navbar-right">
    <LanguageSwitcher />
    <UserAvatar />
  </div>
</nav>
```

#### App Bootstrap

`src/i18n/index.ts` is imported once at the top of `src/main.tsx` before the React tree is rendered, ensuring i18next is initialized before any component calls `useTranslation()`:

```typescript
// src/main.tsx
import './i18n/index';  // initialize i18next before rendering
import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
```

---

### F2 — Language-Aware Recommendations

#### Overview

When the frontend calls an external AI API to generate book recommendations, it must instruct the model to respond in the user's active language. This is achieved through a pure utility function that maps a locale code to a natural-language instruction string, which is then appended to the AI prompt at call time.

#### getLanguageInstruction Utility (`src/utils/getLanguageInstruction.ts`)

```typescript
/**
 * Returns the language instruction to append to AI API prompts
 * based on the active i18next locale.
 *
 * @param locale - The active locale code (e.g., 'pt-BR', 'en-US')
 * @returns A natural-language instruction string for the AI model
 */
export function getLanguageInstruction(locale: string): string {
  switch (locale) {
    case 'pt-BR':
      return 'Responda em português do Brasil.';
    case 'en-US':
      return 'Reply in English (US).';
    default:
      return 'Reply in English (US).';
  }
}
```

**Design decisions:**
- Pure function with no side effects — trivially testable and composable.
- Falls back to English for any unrecognized locale, ensuring the AI always receives a valid instruction.
- No dependency on i18next internals — takes a plain string, making it easy to test in isolation.

#### Integration into AI Prompt Construction

When the recommendation service calls an external AI API, the language instruction is appended to the prompt:

```typescript
// Example usage in recommendation API call
import { getLanguageInstruction } from '../utils/getLanguageInstruction';

async function buildRecommendationPrompt(userHistory: ReadingHistory): Promise<string> {
  const { i18n } = useTranslation(); // or pass locale as parameter
  const languageInstruction = getLanguageInstruction(i18n.language);

  return `Based on the following reading history, recommend 5 books:
${formatReadingHistory(userHistory)}

${languageInstruction}`;
}
```

**Note:** The `getLanguageInstruction` call is made at prompt-construction time (not at component mount), so switching language and then triggering a new recommendation fetch will automatically use the new locale.

#### Rule-Based Recommendation Text

For any recommendation text generated from local/rule-based logic (not an AI API), all strings must use i18n translation keys:

```typescript
// Correct — uses i18n key
const { t } = useTranslation();
<p>{t('recommendations.basedOnRating', { title: book.title })}</p>

// Incorrect — hardcoded string
<p>Based on your rating of {book.title}</p>
```

Corresponding entries must exist in both `pt-BR.json` and `en-US.json`.

---

### F3 — Book Cover Service

#### Overview

The book cover feature consists of two parts: a service module that fetches cover URLs from external APIs, and a UI component that integrates the service into book registration and edit forms. Book cards in list views also display cover thumbnails.

#### bookCoverService (`src/services/bookCoverService.ts`)

```typescript
const OPEN_LIBRARY_COVER_BASE = 'https://covers.openlibrary.org/b/isbn';
const OPEN_LIBRARY_SEARCH_BASE = 'https://openlibrary.org/search.json';

/**
 * Fetches a book cover URL using the ISBN via Open Library Covers API.
 * Returns null on any error (network failure, 404, etc.) — never throws.
 */
export async function fetchCoverByISBN(isbn: string): Promise<string | null> {
  if (!isbn?.trim()) return null;

  const url = `${OPEN_LIBRARY_COVER_BASE}/${encodeURIComponent(isbn.trim())}-L.jpg`;

  try {
    // Open Library returns a 1×1 placeholder for unknown ISBNs.
    // We probe with a HEAD request to check Content-Length before committing.
    const response = await fetch(url, { method: 'HEAD' });
    if (!response.ok) return null;

    const contentLength = response.headers.get('content-length');
    // A valid cover image is always larger than 1 KB; the placeholder is ~807 bytes.
    if (contentLength && parseInt(contentLength, 10) < 1000) return null;

    return url;
  } catch {
    return null;
  }
}

/**
 * Fetches a book cover URL using title and author as fallback lookup keys.
 * Searches Open Library for a matching ISBN, then resolves the cover URL.
 * Returns null on any error — never throws.
 */
export async function fetchCoverByTitleAuthor(
  title: string,
  author: string
): Promise<string | null> {
  if (!title?.trim()) return null;

  try {
    const params = new URLSearchParams({ title: title.trim() });
    if (author?.trim()) params.set('author', author.trim());

    const searchUrl = `${OPEN_LIBRARY_SEARCH_BASE}?${params.toString()}&limit=1&fields=isbn`;
    const response = await fetch(searchUrl);
    if (!response.ok) return null;

    const data = await response.json();
    const firstDoc = data?.docs?.[0];
    const isbn = firstDoc?.isbn?.[0];
    if (!isbn) return null;

    return fetchCoverByISBN(isbn);
  } catch {
    return null;
  }
}
```

**Design decisions:**
- Both functions are `async` and return `Promise<string | null>`. They catch all exceptions internally and return `null` — callers never need to handle errors from this service.
- The HEAD-request probe for `fetchCoverByISBN` avoids displaying Open Library's 1×1 placeholder image as a valid cover.
- `fetchCoverByTitleAuthor` delegates to `fetchCoverByISBN` once it finds a matching ISBN, keeping the cover URL format consistent.
- No caching in Phase 4 — React Query at the component level handles deduplication.

#### BookCoverFetcher Component (`src/components/BookCoverFetcher.tsx`)

```typescript
interface BookCoverFetcherProps {
  isbn?: string;
  title?: string;
  author?: string;
  onCoverFound: (url: string) => void;
}

interface CoverState {
  loading: boolean;
  coverUrl: string | null;
  error: boolean;
}
```

**Component behavior:**

| Trigger | Action |
|---|---|
| ISBN field `onBlur` | Calls `fetchCoverByISBN(isbn)` automatically |
| "Search by title/author" button click | Calls `fetchCoverByTitleAuthor(title, author)` |
| Cover found | Displays 200×300px preview; calls `onCoverFound(url)` |
| Cover not found / error | Displays placeholder with book icon + translated "Cover not found" text |
| Loading | Displays spinner/skeleton in the preview area |

```typescript
export function BookCoverFetcher({ isbn, title, author, onCoverFound }: BookCoverFetcherProps) {
  const { t } = useTranslation();
  const [state, setState] = useState<CoverState>({
    loading: false,
    coverUrl: null,
    error: false,
  });

  const searchByISBN = async () => {
    if (!isbn?.trim()) return;
    setState({ loading: true, coverUrl: null, error: false });
    const url = await fetchCoverByISBN(isbn);
    if (url) {
      setState({ loading: false, coverUrl: url, error: false });
      onCoverFound(url);
    } else {
      setState({ loading: false, coverUrl: null, error: true });
    }
  };

  const searchByTitleAuthor = async () => {
    if (!title?.trim()) return;
    setState({ loading: true, coverUrl: null, error: false });
    const url = await fetchCoverByTitleAuthor(title ?? '', author ?? '');
    if (url) {
      setState({ loading: false, coverUrl: url, error: false });
      onCoverFound(url);
    } else {
      setState({ loading: false, coverUrl: null, error: true });
    }
  };

  return (
    <div className="book-cover-fetcher">
      {/* Preview area: 200×300px */}
      <div
        className="cover-preview"
        style={{ width: 200, height: 300, borderRadius: 8, overflow: 'hidden' }}
      >
        {state.loading && <CoverSkeleton />}
        {!state.loading && state.coverUrl && (
          <img
            src={state.coverUrl}
            alt="Book cover"
            style={{ width: '100%', height: '100%', objectFit: 'cover' }}
          />
        )}
        {!state.loading && !state.coverUrl && (
          <CoverFallback label={t('cover.notFound')} />
        )}
      </div>

      {/* Manual search trigger for title+author */}
      {!isbn && (
        <button type="button" onClick={searchByTitleAuthor} disabled={state.loading}>
          {t('cover.searchByTitle')}
        </button>
      )}
    </div>
  );
}
```

The `onBlur` handler on the ISBN input field in `AdminBookFormPage` calls `searchByISBN` directly:

```typescript
// In AdminBookFormPage.tsx
<input
  name="isbn"
  value={form.isbn}
  onChange={handleChange}
  onBlur={() => coverFetcherRef.current?.searchByISBN()}
/>
<BookCoverFetcher
  ref={coverFetcherRef}
  isbn={form.isbn}
  title={form.title}
  author={selectedAuthorName}
  onCoverFound={(url) => setForm((f) => ({ ...f, coverUrl: url }))}
/>
```

#### Updated BookCard Component

`BookCard` is updated to display a cover thumbnail at 80×120px when a `coverUrl` is available, with a fallback for books without covers:

```typescript
interface BookCardProps {
  book: Book; // Book type updated to include optional coverUrl: string | null
}

function CoverThumbnail({ coverUrl }: { coverUrl?: string | null }) {
  if (coverUrl) {
    return (
      <img
        src={coverUrl}
        alt="Book cover"
        style={{ width: 80, height: 120, objectFit: 'cover', borderRadius: 4 }}
      />
    );
  }
  // Fallback: dark gray background + centered book icon
  return (
    <div
      className="cover-fallback-thumb"
      style={{
        width: 80,
        height: 120,
        backgroundColor: '#2a2a2a',
        borderRadius: 4,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
      }}
      aria-label="No cover available"
    >
      <BookIcon size={32} color="#666" />
    </div>
  );
}

export function BookCard({ book }: BookCardProps) {
  return (
    <div className="book-card">
      <CoverThumbnail coverUrl={book.coverUrl} />
      <div className="book-card-info">
        <h3>{book.title}</h3>
        <p>{book.author.name}</p>
        <p className="isbn">{book.isbn}</p>
      </div>
    </div>
  );
}
```

#### Updated TypeScript Types

```typescript
// src/types/index.ts — updated Book interfaces
export interface Book {
  id: string;
  isbn: string;
  title: string;
  author: { id: string; name: string };
  publisher: { id: string; name: string };
  coverUrl?: string | null;  // NEW — optional cover image URL
  createdAt: string;
}

export interface BookRequest {
  isbn: string;
  title: string;
  authorId: string;
  publisherId: string;
  coverUrl?: string | null;  // NEW — optional, sent to backend on save
}
```

**Note on backend:** The `coverUrl` field is stored as an optional `VARCHAR` column on the `books` table (added via a new Flyway migration `V7__add_cover_url_to_books.sql`). The backend `BookRequest` and `BookResponse` DTOs are updated to include the optional `coverUrl` field. No backend business logic changes are required — the field is stored and returned as-is.

---

### F4 — Login Screen Redesign

#### Overview

The login screen is redesigned with a split-screen layout: a visually rich left panel (60% width) featuring a library background image, gradient overlay, and an inspirational quote; and a clean right panel (40% width) containing the existing login form with the GMLib logo above it.

#### Layout Structure

```
┌─────────────────────────────────────────────────────────────┐
│                    LoginPage (full viewport)                  │
├──────────────────────────────┬──────────────────────────────┤
│   Left Panel (60%)           │   Right Panel (40%)          │
│                              │                              │
│   [background image]         │   [GMLib Logo]               │
│   + gradient overlay         │                              │
│                              │   [Login Form]               │
│   [Inspirational Quote]      │   - Email field              │
│   italic, serif, white       │   - Password field           │
│   opacity 0.85, 18px         │   - Submit button            │
│                              │   - Register link            │
└──────────────────────────────┴──────────────────────────────┘
```

#### LoginPage Component (`src/pages/LoginPage.tsx` — updated)

```typescript
export function LoginPage() {
  const { t } = useTranslation();

  return (
    <div className="login-page">
      {/* Left panel — hidden on mobile */}
      <div className="login-left-panel" aria-hidden="true">
        <div className="login-bg-image" />
        <div className="login-overlay" />
        <blockquote className="login-quote">
          {t('login.quote')}
        </blockquote>
      </div>

      {/* Right panel — always visible */}
      <div className="login-right-panel">
        <img src={gmLibLogo} alt="GMLib" className="login-logo" />
        <LoginForm />
      </div>
    </div>
  );
}
```

#### CSS Design

```css
/* Split-screen layout */
.login-page {
  display: flex;
  min-height: 100vh;
}

/* Left panel */
.login-left-panel {
  position: relative;
  flex: 0 0 60%;
  overflow: hidden;
}

.login-bg-image {
  position: absolute;
  inset: 0;
  background-image: url('/src/assets/images/login-bg.jpg');
  background-size: cover;
  background-position: center;
}

.login-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(0, 0, 0, 0.7), rgba(224, 112, 32, 0.15));
}

.login-quote {
  position: relative; /* above overlay */
  z-index: 1;
  font-style: italic;
  font-family: Georgia, 'Times New Roman', serif;
  color: rgba(255, 255, 255, 0.85);
  font-size: 18px;
  line-height: 1.6;
  padding: 2rem;
  margin: auto 0 3rem;
}

/* Right panel */
.login-right-panel {
  flex: 0 0 40%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background-color: #1a1a1a;
  padding: 2rem;
}

.login-logo {
  width: 120px;
  margin-bottom: 2rem;
}

/* Mobile: hide left panel, use image as blurred background */
@media (max-width: 767px) {
  .login-left-panel {
    display: none;
  }

  .login-right-panel {
    flex: 1;
    position: relative;
    background-image: url('/src/assets/images/login-bg.jpg');
    background-size: cover;
    background-position: center;
  }

  .login-right-panel::before {
    content: '';
    position: absolute;
    inset: 0;
    backdrop-filter: blur(8px);
    background-color: rgba(0, 0, 0, 0.6);
  }

  .login-right-panel > * {
    position: relative;
    z-index: 1;
  }
}
```

#### Background Image Fallback

If the background image fails to load (external URL unavailable or local file missing), the CSS `background-image` property simply renders nothing, and the `background-color` on `.login-left-panel` provides the fallback:

```css
.login-left-panel {
  background-color: #1a1a1a; /* fallback if image unavailable */
}
```

The gradient overlay and quote remain visible regardless of whether the image loads, ensuring the layout never breaks.

#### Responsive Behavior Summary

| Viewport | Left Panel | Right Panel |
|---|---|---|
| ≥ 768px | Visible (60%), background image + overlay + quote | Visible (40%), dark bg, logo + form |
| < 768px | Hidden (`display: none`) | Full width, blurred background image behind form |
| Image unavailable | Solid dark background (`#1a1a1a`) | Unaffected |

---

## New Correctness Properties (F1–F4)

### Property 28: Language switcher persists preference to localStorage

*For any* locale value in `{'pt-BR', 'en-US'}`, when the user selects that locale via the `LanguageSwitcher`, the value stored in `localStorage` under the key `i18n_language` SHALL equal the selected locale code, and on the next application load the interface SHALL initialize in that locale.

**Validates: Requirements 13.4, 13.2**

---

### Property 29: getLanguageInstruction returns correct string for each locale

*For any* call to `getLanguageInstruction(locale)` where `locale` is `'pt-BR'`, the function SHALL return exactly `"Responda em português do Brasil."`. *For any* call where `locale` is `'en-US'`, the function SHALL return exactly `"Reply in English (US)."`. The function SHALL never throw an exception for any string input.

**Validates: Requirements 14.2, 14.3, 14.4**

---

### Property 30: bookCoverService returns null on network error, never throws

*For any* call to `fetchCoverByISBN(isbn)` or `fetchCoverByTitleAuthor(title, author)` where the underlying network request fails (timeout, DNS failure, HTTP 4xx/5xx, malformed response), the function SHALL return `null` and SHALL NOT throw an unhandled exception or cause the calling component to enter an error state.

**Validates: Requirements 15.11**

---

### Property 31: BookCoverFetcher shows fallback when no cover found

*For any* render of `BookCoverFetcher` where the cover service returns `null` (cover not found) or encounters an error, the component SHALL display the fallback placeholder (book icon + translated "Cover not found" text) and SHALL NOT render a broken `<img>` element. Equivalently, *for any* `BookCard` rendered with `coverUrl` equal to `null` or `undefined`, the component SHALL display the dark gray fallback thumbnail rather than a broken image.

**Validates: Requirements 15.5, 15.13**

---
