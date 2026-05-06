# Implementation Plan: Personal Library Manager

## Overview

Incremental implementation across three phases. Phase 1 builds the full backend foundation (Quarkus modular monolith) and the React frontend for catalog browsing and admin management. Phase 2 adds the reading module end-to-end. Phase 3 is an optional future extraction of the recommendation service.

Each task builds on the previous. Property-based tests use **jqwik** (`@Property`, `@ForAll`). Integration tests use **Quarkus `@QuarkusTest` + REST Assured + Testcontainers**. Frontend tests use **Vitest + React Testing Library + MSW**.

---

## Phase 1 — Foundation

- [x] 1. Initialize backend project structure
  - Generate a Quarkus 3.x Maven project with extensions: `quarkus-resteasy-reactive-jackson`, `quarkus-hibernate-orm-panache`, `quarkus-jdbc-postgresql`, `quarkus-flyway`, `quarkus-smallrye-jwt`, `quarkus-smallrye-health`, `quarkus-arc`
  - Add test-scope dependencies: `quarkus-junit5`, `rest-assured`, `quarkus-test-security`, `testcontainers-postgresql`, `jqwik`
  - Add JaCoCo Maven plugin configured for ≥ 80% line coverage on `com.library.*` service and domain classes
  - Create the root package structure: `com.library.{identity,catalog,reading,recommendation,shared}`
  - Add `application.properties` with Flyway (`quarkus.flyway.migrate-at-start=true`), datasource, JWT issuer (`personal-library-manager`), and log format including `%X{correlationId}`
  - Generate RS256 key pair; place `privateKey.pem` and `publicKey.pem` under `src/main/resources/META-INF/resources/`
  - _Requirements: NFR 1.1, NFR 3.1, 12.4_

- [x] 2. Implement shared module
  - [x] 2.1 Create `DomainEvent`, `DomainEventEnvelope` record, and `EventBus` interface in `com.library.shared.event`
    - `DomainEventEnvelope(UUID eventId, String eventType, Instant occurredAt, Object payload)`
    - _Requirements: 9.1, 9.9, 9.10_
  - [x] 2.2 Implement `InProcessEventBus` using CDI `Event<DomainEventEnvelope>`
    - `@ApplicationScoped` bean; `publish()` calls `cdiEvent.fire(envelope)`
    - _Requirements: 9.10_
  - [x] 2.3 Create `PageRequest` and `PageResponse<T>` records in `com.library.shared.pagination`
    - `PageResponse<T>(List<T> content, long totalElements, int totalPages, int page, int size)`
    - _Requirements: 11.1, 11.3_
  - [x] 2.4 Create `AppException` hierarchy in `com.library.shared.exception`
    - `AppException(RuntimeException)` → `NotFoundException(404)`, `ConflictException(409)`, `ValidationException(400)`, `ForbiddenException(403)`, `UnprocessableEntityException(422)`, `UnauthorizedException(401)`
    - _Requirements: 12.1_
  - [x] 2.5 Implement `GlobalExceptionMapper` (`ExceptionMapper<Throwable>`) and `ErrorResponse` record
    - Map `AppException` subclasses to their HTTP status; map `ConstraintViolationException` to 400; catch-all to 500 with ERROR log + correlationId
    - _Requirements: 12.1, 12.2, 12.3_
  - [x] 2.6 Implement `CorrelationIdFilter` (`ContainerRequestFilter` + `ContainerResponseFilter`)
    - Read `X-Correlation-ID` header or generate UUID; store in MDC; echo in response header
    - _Requirements: 12.4_

- [x] 3. Write Flyway migrations V1–V5
  - [x] 3.1 `V1__create_users.sql` — `users` table with UUID PK, email UNIQUE index, role CHECK constraint
    - _Requirements: 1.1, 1.5_
  - [x] 3.2 `V2__create_profiles.sql` — `profiles` table with FK → users ON DELETE CASCADE, unique user_id
    - _Requirements: 4.1_
  - [x] 3.3 `V3__create_authors.sql` — `authors` table
    - _Requirements: 5.1_
  - [x] 3.4 `V4__create_publishers.sql` — `publishers` table
    - _Requirements: 6.1_
  - [x] 3.5 `V5__create_books.sql` — `books` table with FK → authors, FK → publishers, ISBN UNIQUE, GIN full-text index on title
    - _Requirements: 7.1, 7.2_

- [x] 4. Implement identity module — domain and infrastructure
  - [x] 4.1 Create `User` and `Profile` JPA entities and `Role` enum in `com.library.identity.domain`
    - Extend `PanacheEntityBase`; UUID PK with `GenerationType.UUID`; `@PrePersist`/`@PreUpdate` for timestamps
    - _Requirements: 1.1, 4.1, NFR 3.1_
  - [x] 4.2 Implement `UserRepository` and `ProfileRepository` interfaces and their Panache implementations
    - `findByEmail(String)`, `findByIdOrThrow(UUID)`, `persist(User)` / `findByUserId(UUID)`, `persist(Profile)`
    - _Requirements: 1.1, 4.3_
  - [x] 4.3 Implement `BcryptPasswordEncoder` (bcrypt cost 12) and `JwtIssuer`
    - `JwtIssuer.issue(User)` builds claims: `sub`, `email`, `groups`, `iss`, `exp = iat + 3600`
    - _Requirements: 1.5, 2.1, 2.6, NFR 1.2_

- [x] 5. Implement identity module — application layer
  - [x] 5.1 Implement `AuthService`: `register()` and `login()`
    - `register`: validate email uniqueness (→ 409), validate password ≥ 8 chars (→ 400), hash password, persist User, create empty Profile, publish `user.created` event, return `UserResponse`
    - `login`: find user by email (→ 401 if absent), verify bcrypt hash (→ 401 if mismatch), issue JWT, return `LoginResponse`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 2.1, 2.2, 2.3_
  - [x] 5.2 Implement `UserService`: `getCurrentUser()` and `updateRole()`
    - `updateRole` is admin-only (enforced at resource layer with `@RolesAllowed("ADMIN")`)
    - _Requirements: 3.6_
  - [x] 5.3 Implement `ProfileService`: `getProfile()` and `updateProfile()`
    - `getProfile`: throw `ForbiddenException` if `requestingUserId != profileOwnerId`
    - `updateProfile`: validate displayName ≤ 100, bio ≤ 1000 (→ 400); persist and return updated profile
    - _Requirements: 4.2, 4.3, 4.4, 4.5, 4.6_
  - [ ]* 5.4 Write property tests for `AuthService` (jqwik)
    - **Property 1: Valid registration always produces a USER-role account** — Validates: Requirements 1.1
    - **Property 2: Invalid registration input is rejected** — Validates: Requirements 1.3, 1.4
    - **Property 3: Duplicate email registration is rejected** — Validates: Requirements 1.2
    - **Property 4: Password is stored as bcrypt hash** — Validates: Requirements 1.5, NFR 1.2
    - **Property 5: Successful registration publishes user.created event** — Validates: Requirements 1.6, 9.2
    - **Property 6: Correct credentials always yield a valid JWT** — Validates: Requirements 2.1
    - **Property 7: Incorrect credentials always yield 401** — Validates: Requirements 2.2, 2.3
  - [ ]* 5.5 Write property tests for `ProfileService` (jqwik)
    - **Property 9: Profile is auto-created on user registration** — Validates: Requirements 4.1
    - **Property 10: Profile update is a round-trip** — Validates: Requirements 4.2, 4.3
    - **Property 11: Profile field length violations are rejected** — Validates: Requirements 4.5, 4.6
    - **Property 12: Cross-user profile access is forbidden** — Validates: Requirements 4.4

- [x] 6. Implement identity module — API layer
  - [x] 6.1 Create DTOs and mappers: `RegisterRequest`, `LoginRequest`, `UserResponse`, `LoginResponse`, `ProfileResponse`, `ProfileUpdateRequest`
    - Use Jakarta Validation annotations (`@NotBlank`, `@Email`, `@Size`) on request DTOs
    - _Requirements: NFR 3.2, NFR 3.3_
  - [x] 6.2 Implement `AuthResource` (`POST /api/v1/auth/register`, `POST /api/v1/auth/login`) — no auth required
    - _Requirements: 1.1, 2.1_
  - [x] 6.3 Implement `UserResource` (`GET /api/v1/users/me`, `PUT /api/v1/users/{id}/role`)
    - `PUT /role` annotated `@RolesAllowed("ADMIN")`
    - _Requirements: 3.5, 3.6_
  - [x] 6.4 Implement `ProfileResource` (`GET /api/v1/users/me/profile`, `PUT /api/v1/users/me/profile`)
    - Both endpoints `@RolesAllowed({"USER","ADMIN"})`
    - _Requirements: 4.2, 4.3_

- [ ] 7. Checkpoint — identity module
  - Ensure all unit and property tests pass. Verify Flyway migrations V1–V2 apply cleanly against a Testcontainers PostgreSQL instance. Ask the user if questions arise.

- [x] 8. Implement catalog module — domain and infrastructure
  - [x] 8.1 Create `Author`, `Publisher`, `Book` JPA entities in `com.library.catalog.domain`
    - `Book` has `@ManyToOne(fetch = LAZY)` to `Author` and `Publisher`; ISBN unique constraint
    - _Requirements: 7.1, NFR 3.1_
  - [x] 8.2 Implement `AuthorRepository`, `PublisherRepository`, `BookRepository` interfaces and Panache implementations
    - `BookRepository.findByIsbn(String)`, `search(String query, PageRequest)` using JPQL LOWER LIKE join on Author
    - _Requirements: 7.5, 7.12_

- [x] 9. Implement catalog module — application layer
  - [x] 9.1 Implement `AuthorService`: create, update, delete, list, getById
    - Delete: check for associated books → 409 if any exist; validate non-blank name → 400
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7_
  - [x] 9.2 Implement `PublisherService`: create, update, delete, list, getById
    - Same delete guard as AuthorService
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7_
  - [x] 9.3 Implement `BookService`: create, update, delete, list/search, getById
    - Create: validate ISBN uniqueness (→ 409), validate authorId exists (→ 422), validate publisherId exists (→ 422)
    - Publish `book.created`, `book.updated`, `book.deleted` events via `EventBus`
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7, 7.8, 7.9, 7.10, 7.11, 7.12_
  - [ ]* 9.4 Write property tests for `AuthorService` / `PublisherService` (jqwik)
    - **Property 13: Author/Publisher with blank name is rejected** — Validates: Requirements 5.7, 6.7
    - **Property 14: Author with associated books cannot be deleted** — Validates: Requirements 5.3, 5.4, 6.3, 6.4
  - [ ]* 9.5 Write property tests for `BookService` (jqwik)
    - **Property 8: Non-admin users are forbidden from catalog mutations** — Validates: Requirements 3.1, 3.2, 3.3
    - **Property 15: Book creation is a round-trip** — Validates: Requirements 7.1
    - **Property 16: Duplicate ISBN is rejected** — Validates: Requirements 7.2
    - **Property 17: Book search returns only matching results** — Validates: Requirements 7.12, 11.4
    - **Property 18: Catalog events are published on book mutations** — Validates: Requirements 7.9, 7.10, 7.11, 9.3, 9.4, 9.5

- [x] 10. Implement catalog module — API layer
  - [x] 10.1 Create DTOs and mappers: `AuthorRequest`, `AuthorResponse`, `PublisherRequest`, `PublisherResponse`, `BookRequest`, `BookResponse`
    - `BookResponse` embeds `AuthorResponse` and `PublisherResponse` (no raw FK IDs)
    - _Requirements: NFR 3.2, NFR 3.3_
  - [x] 10.2 Implement `AuthorResource` — full CRUD at `/api/v1/authors`
    - POST/PUT/DELETE: `@RolesAllowed("ADMIN")`; GET: `@RolesAllowed({"USER","ADMIN"})`
    - _Requirements: 3.1, 3.2, 5.1–5.7_
  - [x] 10.3 Implement `PublisherResource` — full CRUD at `/api/v1/publishers`
    - Same role guards as AuthorResource
    - _Requirements: 3.3, 6.1–6.7_
  - [x] 10.4 Implement `BookResource` — full CRUD at `/api/v1/books` with `search`, `page`, `size` query params
    - Validate `size` ≤ 100 (→ 400); default page=0, size=20
    - _Requirements: 7.1–7.12, 11.1, 11.2, 11.3, 11.4_
  - [ ]* 10.5 Write property tests for pagination (jqwik)
    - **Property 25: Paginated responses always include pagination metadata** — Validates: Requirements 11.1, 11.3
    - **Property 26: Oversized page size is rejected** — Validates: Requirements 11.2

- [x] 11. Implement recommendation stub module
  - Create `RecommendationRequest` and `RecommendationResponse` domain contracts in `com.library.recommendation.domain`
  - Implement `RecommendationResource` returning `new RecommendationResponse(List.of())` at `GET /api/v1/recommendations` (`@Authenticated`)
  - _Requirements: 10.3, 10.4, 10.5_

- [x] 12. Checkpoint — catalog and shared modules
  - Ensure all unit, property, and mapper tests pass. Verify Flyway migrations V3–V5 apply cleanly. Ask the user if questions arise.

- [x] 13. Backend integration tests — Phase 1
  - [x] 13.1 Write `@QuarkusTest` integration tests for identity endpoints using REST Assured + Testcontainers
    - Cover: register (201, 409, 400), login (200, 401), `GET /users/me`, `PUT /users/{id}/role` (403 for USER), profile CRUD
    - Verify JWT claims in login response; verify `user.created` event published via test CDI observer
    - _Requirements: 1.1–1.6, 2.1–2.6, 3.6, 4.1–4.6, NFR 4.2_
  - [x] 13.2 Write `@QuarkusTest` integration tests for catalog endpoints
    - Cover: author/publisher/book CRUD (201, 200, 204, 404, 409, 422, 403), search, pagination
    - Verify `book.created/updated/deleted` events via test CDI observer
    - _Requirements: 5.1–5.7, 6.1–6.7, 7.1–7.12, 11.1–11.4, NFR 4.2_
  - [x] 13.3 Write `@QuarkusTest` integration test for `GET /q/health`
    - Verify application and DB connection status reported
    - _Requirements: 12.5_
  - [ ]* 13.4 Write property tests for error response structure (jqwik)
    - **Property 27: All error responses have a consistent structure** — Validates: Requirements 12.1

- [x] 14. Initialize frontend project
  - Scaffold with `npm create vite@latest frontend -- --template react-ts`
  - Install dependencies: `axios`, `react-router-dom`, `@tanstack/react-query`
  - Install dev dependencies: `vitest`, `@testing-library/react`, `@testing-library/user-event`, `msw`, `@types/react`, `@types/react-dom`
  - Configure `vite.config.ts` with proxy to `http://localhost:8080` for `/api`
  - Create `src/types/index.ts` with TypeScript interfaces matching all API DTOs (`User`, `Profile`, `Author`, `Publisher`, `Book`, `PageResponse<T>`, `LoginResponse`, `ErrorResponse`)
  - _Requirements: NFR 3.2_

- [x] 15. Implement frontend auth infrastructure
  - [x] 15.1 Implement `src/api/client.ts` — Axios instance with base URL from `VITE_API_BASE_URL`, request interceptor attaching JWT from `localStorage`, response interceptor redirecting to `/login` on 401
    - _Requirements: 2.4, 2.5_
  - [x] 15.2 Implement `src/auth/AuthContext.tsx` — `AuthState` with `user`, `login(token)`, `logout()`
    - `login`: decode JWT payload (base64), store in `localStorage`, set user state
    - `logout`: clear storage, redirect to `/login`
    - _Requirements: 2.1_
  - [x] 15.3 Implement `src/auth/ProtectedRoute.tsx` — renders `<Outlet />` if authenticated; redirects to `/login` if not; redirects to `/books` if wrong role
    - _Requirements: 3.1, 3.4_
  - [ ]* 15.4 Write Vitest unit tests for `AuthContext` and `ProtectedRoute`
    - Test login/logout state transitions; test redirect behavior for unauthenticated and wrong-role users
    - _Requirements: 2.4, 3.1_

- [x] 16. Implement frontend API modules and App router
  - Create `src/api/auth.ts`, `src/api/books.ts`, `src/api/authors.ts`, `src/api/publishers.ts` with typed functions wrapping the Axios client
  - Implement `src/App.tsx` with `BrowserRouter` + `Routes` matching the full route map (public, USER-protected, ADMIN-protected)
  - _Requirements: NFR 3.2_

- [x] 17. Implement frontend Phase 1 screens
  - [x] 17.1 Implement `LoginPage.tsx` and `RegisterPage.tsx`
    - Forms with client-side validation (non-empty fields, email format, password ≥ 8 chars); call `auth.ts` API functions; store JWT via `AuthContext.login()`; redirect to `/books` on success
    - _Requirements: 1.1, 1.3, 1.4, 2.1_
  - [x] 17.2 Implement shared components: `Navbar.tsx`, `Pagination.tsx`, `BookCard.tsx`
    - `Navbar` shows user email and logout button; `Pagination` emits page-change events; `BookCard` displays title, author, ISBN
    - _Requirements: 11.1_
  - [x] 17.3 Implement `BookListPage.tsx`
    - Fetch `GET /books?page=&size=&search=` via React Query; render search input, `BookCard` list, `Pagination`; debounce search input
    - _Requirements: 7.5, 11.1, 11.4_
  - [x] 17.4 Implement `BookDetailPage.tsx`
    - Fetch `GET /books/:id`; if no reading exists show "Add to Library" button calling `POST /readings`; if reading exists show status/rating/review
    - _Requirements: 7.6, 8.1_
  - [x] 17.5 Implement admin screens: `AdminAuthorListPage.tsx`, `AdminAuthorFormPage.tsx`, `AdminPublisherListPage.tsx`, `AdminPublisherFormPage.tsx`, `AdminBookFormPage.tsx`
    - Author/Publisher list pages with create/edit/delete actions; form pages with controlled inputs and submit handlers calling the respective API modules
    - `AdminBookFormPage` fetches authors and publishers for `<select>` dropdowns
    - _Requirements: 5.1–5.7, 6.1–6.7, 7.1–7.4_
  - [ ]* 17.6 Write Vitest + MSW integration tests for `BookListPage` and admin forms
    - Mock API responses; test search filtering, pagination navigation, form submission success and error states
    - _Requirements: 7.5, 11.4_

- [~] 18. Final Phase 1 checkpoint
  - Ensure all backend unit, property, and integration tests pass and JaCoCo reports ≥ 80% line coverage on service/domain classes. Ensure all frontend Vitest tests pass. Ask the user if questions arise.

---

## Phase 2 — Reading Module

- [x] 19. Write Flyway migration V6
  - `V6__create_readings.sql` — `readings` table with UNIQUE(user_id, book_id), rating CHECK (0.0–10.0), status CHECK, `chk_finished_after_started` constraint
  - _Requirements: 8.1, 8.2, 8.9_

- [x] 20. Implement reading module — domain and infrastructure
  - [x] 20.1 Create `Reading` JPA entity and `ReadingStatus` enum in `com.library.reading.domain`
    - Fields: id, userId (FK → users), bookId (FK → books), status, rating, review, startedAt, finishedAt, createdAt, updatedAt
    - _Requirements: 8.1, NFR 3.1_
  - [x] 20.2 Implement `ReadingRepository` interface and Panache implementation
    - `findByUserIdAndBookId(UUID, UUID)`, `findByIdOrThrow(UUID)`, `findByUserId(UUID, ReadingStatus, PageRequest)`, `persist`, `delete`
    - _Requirements: 8.6_

- [x] 21. Implement reading module — application layer
  - [x] 21.1 Implement `ReadingService`: create, update, delete, listForUser, getById
    - `create`: check duplicate (userId + bookId → 409); set status = WANT_TO_READ; publish `reading.created`
    - `update`: ownership check (→ 403); validate rating ∈ [0.0, 10.0] (→ 400); validate FINISHED requires startedAt (→ 400); publish `rating.updated` if rating changed, `review.submitted` if review changed
    - `delete`: ownership check (→ 403)
    - `listForUser`: filter by status if provided; paginate
    - _Requirements: 8.1–8.14, 9.6, 9.7, 9.8_
  - [ ]* 21.2 Write property tests for `ReadingService` (jqwik)
    - **Property 19: New reading always starts with WANT_TO_READ** — Validates: Requirements 8.1
    - **Property 20: Duplicate reading is rejected** — Validates: Requirements 8.2
    - **Property 21: Reading update is a round-trip** — Validates: Requirements 8.3
    - **Property 22: Out-of-range rating is rejected** — Validates: Requirements 8.9
    - **Property 23: Cross-user reading mutation is forbidden** — Validates: Requirements 8.5, 8.8
    - **Property 24: Reading events are published on reading mutations** — Validates: Requirements 8.11, 8.12, 8.13, 9.6, 9.7, 9.8

- [x] 22. Implement reading module — API layer
  - [x] 22.1 Create DTOs and mappers: `CreateReadingRequest`, `UpdateReadingRequest`, `ReadingResponse`
    - `ReadingResponse` embeds `BookSummary(id, title)` — no raw FK IDs in response
    - _Requirements: NFR 3.2, NFR 3.3_
  - [x] 22.2 Implement `ReadingResource` — full CRUD at `/api/v1/readings`
    - All endpoints `@RolesAllowed({"USER","ADMIN"})`; extract `userId` from JWT `@Context SecurityContext`
    - Pass `isAdmin` flag to service for ownership bypass
    - _Requirements: 8.1–8.14, 11.1, 11.5_

- [x] 23. Backend integration tests — Phase 2
  - [x] 23.1 Write `@QuarkusTest` integration tests for reading endpoints
    - Cover: create (201, 409), update (200, 400 rating, 400 FINISHED without startedAt, 403 cross-user), delete (204, 403), list with status filter and pagination, getById (200, 403)
    - Verify `reading.created`, `rating.updated`, `review.submitted` events via test CDI observer
    - _Requirements: 8.1–8.14, 9.6–9.8, 11.5, NFR 4.2_

- [x] 24. Implement frontend Phase 2 screens
  - [x] 24.1 Create `src/api/readings.ts` with typed functions for reading CRUD
    - _Requirements: 8.1, 8.3, 8.14_
  - [x] 24.2 Implement `ReadingStatusBadge.tsx` and `StarRating.tsx` shared components
    - `ReadingStatusBadge` renders colored badge per status; `StarRating` renders 0–10 slider with numeric display
    - _Requirements: 8.3_
  - [x] 24.3 Implement `MyReadingsPage.tsx`
    - Fetch `GET /readings?status=&page=&size=` via React Query; render status filter tabs, reading list rows with `ReadingStatusBadge` and book title; `Pagination`
    - _Requirements: 8.6, 11.1, 11.5_
  - [x] 24.4 Implement `ReadingDetailPage.tsx`
    - Fetch `GET /readings/:id`; form for updating status (select), rating (`StarRating`), review (textarea), startedAt/finishedAt (date inputs); submit calls `PUT /readings/:id`; show validation errors inline
    - _Requirements: 8.3, 8.9, 8.10_
  - [ ]* 24.5 Write Vitest + MSW integration tests for `MyReadingsPage` and `ReadingDetailPage`
    - Test status filter tab switching, form submission with valid and invalid data (out-of-range rating, FINISHED without startedAt)
    - _Requirements: 8.6, 8.9, 8.10_

- [~] 25. Final Phase 2 checkpoint
  - Ensure all backend and frontend tests pass. Verify JaCoCo coverage still ≥ 80%. Ask the user if questions arise.

---

## Phase 3 — Recommendation Service (Optional / Future)

- [x] 26. Extract recommendation service as standalone deployable
  - Create a new Maven module (or separate project) `recommendation-service` with its own Quarkus bootstrap
  - Copy `RecommendationRequest` / `RecommendationResponse` contracts from the monolith's `recommendation/domain`
  - _Requirements: 10.1, 10.2, 10.3_

- [x] 27. Connect recommendation service to message broker
  - Add `quarkus-smallrye-reactive-messaging-kafka` (or SQS connector) to the recommendation service
  - Implement `KafkaEventBus` as `@Alternative @Priority(1)` in the monolith to replace `InProcessEventBus`
  - Configure consumer in recommendation service to subscribe to `reading-events` topic
  - _Requirements: 9.1, 10.2_

- [x] 28. Implement LLM integration in recommendation service
  - Implement `RecommendationService` that reads `rating.updated` and `review.submitted` events, builds a prompt from user history, calls the configured LLM API, and stores recommendations
  - Expose `GET /api/v1/recommendations` returning personalized book suggestions
  - _Requirements: 10.2, 10.4_

- [x] 29. Implement frontend Recommendations page
  - Implement `RecommendationsPage.tsx` fetching `GET /recommendations` via React Query; render book suggestion cards; add route `/recommendations` under `ProtectedRoute`
  - _Requirements: 10.4_

- [x] 30. Final Phase 3 checkpoint
  - Ensure recommendation service tests pass. Verify monolith still returns empty list when recommendation service is not deployed. Ask the user if questions arise.

---

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP
- Property tests use jqwik `@Property(tries = 100)` with generators as described in the design's Testing Strategy section
- Each property test must include the tag comment: `// Feature: personal-library-manager, Property N: <property text>`
- Integration tests use `@QuarkusTest` + Testcontainers; the PostgreSQL container is started once per test suite via a shared `@QuarkusTestResource`
- Frontend MSW handlers should mirror the exact API contracts defined in requirements.md
- JaCoCo coverage is enforced at the Maven `verify` phase; the build fails if coverage drops below 80% on service and domain classes
