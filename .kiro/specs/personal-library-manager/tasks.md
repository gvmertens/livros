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

---

## Phase 4 — Frontend Enhancements (F1–F4)

- [x] 31. Set up i18n infrastructure (F1)
  - Install `react-i18next`, `i18next`, and `i18next-browser-languagedetector` as dependencies in `frontend/`
  - Create `src/i18n/index.ts` with i18next configuration: `LanguageDetector` plugin, `initReactI18next`, `fallbackLng: 'pt-BR'`, `supportedLngs: ['pt-BR', 'en-US']`, detection order `['localStorage', 'navigator']`, `lookupLocalStorage: 'i18n_language'`
  - Create `src/i18n/locales/pt-BR.json` with all UI strings in Brazilian Portuguese, organized by feature area: `nav`, `auth`, `books`, `cover`, `login`, `recommendations`, `readings`, `admin`, `common` (buttons, errors, empty states, validation messages)
  - Create `src/i18n/locales/en-US.json` with the same key structure, all strings in American English
  - Import `src/i18n/index.ts` at the top of `src/main.tsx` before the React tree renders
  - _Requirements: 13.1, 13.2, 13.10, 13.11_

- [x] 32. Implement LanguageSwitcher component and Navbar integration (F1)
  - [x] 32.1 Create `src/components/LanguageSwitcher.tsx` — inline "PT | EN" toggle using `useTranslation()` from `react-i18next`
    - Active language highlighted in brand orange `#E07020` with `font-weight: 600`
    - Each button has `aria-pressed` set to `true`/`false` based on active locale
    - Clicking a language calls `i18n.changeLanguage(locale)` — no dropdown, direct toggle
    - _Requirements: 13.3, 13.4, 13.7, 13.8, 13.9_
  - [x] 32.2 Update `src/components/Navbar.tsx` to render `<LanguageSwitcher />` on the right side, before any avatar or profile element
    - Replace all hardcoded strings in `Navbar.tsx` with `t()` calls using the `nav.*` translation keys
    - _Requirements: 13.6, 13.7_
  - [ ]* 32.3 Write Vitest unit test for `LanguageSwitcher`
    - Test that clicking "EN" calls `i18n.changeLanguage('en-US')` and sets `aria-pressed="true"` on the EN button
    - Test that clicking "PT" calls `i18n.changeLanguage('pt-BR')` and sets `aria-pressed="true"` on the PT button
    - **Property 28: Language switcher persists preference to localStorage**
    - **Validates: Requirements 13.4, 13.2**

- [x] 33. Migrate all existing components and pages to use i18n (F1)
  - [x] 33.1 Update all pages under `src/pages/` to replace hardcoded static text with `useTranslation()` and `t()` calls
    - Pages to update: `RegisterPage.tsx`, `BookListPage.tsx`, `BookDetailPage.tsx`, `MyReadingsPage.tsx`, `ReadingDetailPage.tsx`, `ProfilePage.tsx`
    - Replace form labels, placeholders, button labels, page titles, empty-state messages, and validation error messages
    - _Requirements: 13.5, 13.6_
  - [x] 33.2 Update all admin pages under `src/pages/admin/` to use `t()` for all static text
    - Pages to update: `AdminBookFormPage.tsx`, `AdminAuthorListPage.tsx`, `AdminAuthorFormPage.tsx`, `AdminPublisherListPage.tsx`, `AdminPublisherFormPage.tsx`
    - _Requirements: 13.5, 13.6_
  - [x] 33.3 Update shared components `src/components/BookCard.tsx`, `src/components/Pagination.tsx`, `src/components/ReadingStatusBadge.tsx`, `src/components/StarRating.tsx` to use `t()` for any static text
    - _Requirements: 13.5, 13.6_

- [x] 34. Checkpoint — i18n baseline
  - Ensure all Vitest tests pass. Verify that switching language in the browser updates all visible UI text immediately without a page reload. Ask the user if questions arise.

- [ ] 35. Implement getLanguageInstruction utility (F2)
  - [ ] 35.1 Create `src/utils/getLanguageInstruction.ts` — pure function `getLanguageInstruction(locale: string): string`
    - Returns `"Responda em português do Brasil."` for `'pt-BR'`
    - Returns `"Reply in English (US)."` for `'en-US'`
    - Returns `"Reply in English (US)."` as default for any unrecognized locale
    - _Requirements: 14.2, 14.3, 14.4_
  - [ ]* 35.2 Write Vitest property test for `getLanguageInstruction`
    - **Property 29: getLanguageInstruction returns correct string for each locale**
    - Test `'pt-BR'` → exact string `"Responda em português do Brasil."`
    - Test `'en-US'` → exact string `"Reply in English (US)."`
    - Test arbitrary string inputs never throw an exception
    - **Validates: Requirements 14.2, 14.3, 14.4**
  - [ ] 35.3 Update the recommendation API call in `src/api/` (or `src/pages/`) to append `getLanguageInstruction(i18n.language)` to the AI prompt when calling the external recommendation API
    - Ensure all recommendation display text in components uses `t()` keys, not hardcoded strings
    - _Requirements: 14.1, 14.3, 14.4, 14.5, 14.6_

- [ ] 36. Redesign LoginPage with split-screen layout (F4)
  - [ ] 36.1 Add `src/assets/images/login-bg.jpg` — download a library/bookshelf image from Unsplash or reference a public URL; place the file in the assets directory
    - _Requirements: 16.1, 16.7_
  - [ ] 36.2 Redesign `src/pages/LoginPage.tsx` with a two-panel flex layout
    - Left panel (`flex: 0 0 60%`): background image via CSS `background-image`, gradient overlay `linear-gradient(135deg, rgba(0,0,0,0.7), rgba(224,112,32,0.15))`, and `<blockquote>` with `t('login.quote')` in italic serif font, white at 0.85 opacity, 18px
    - Right panel (`flex: 0 0 40%`): dark background `#1a1a1a`, GMLib logo centered above the existing login form
    - Left panel has `aria-hidden="true"` since it is decorative
    - _Requirements: 16.1, 16.2, 16.3, 16.4, 16.5, 16.6, 16.9, 16.10_
  - [ ] 36.3 Add responsive CSS: at `max-width: 767px`, hide the left panel (`display: none`) and apply the background image as a blurred full-screen background behind the right panel using `backdrop-filter: blur(8px)` and a dark overlay
    - Add `background-color: #1a1a1a` fallback on the left panel for when the image is unavailable
    - _Requirements: 16.8, 16.9_
  - [ ]* 36.4 Write Vitest unit test for `LoginPage`
    - Test that the quote renders the PT-BR text when locale is `pt-BR`
    - Test that the quote renders the EN-US text when locale is `en-US`
    - Test that the login form fields and submit button are present and functional
    - _Requirements: 16.4, 16.5, 16.10_

- [ ] 37. Checkpoint — Login screen and i18n integration
  - Ensure all Vitest tests pass. Verify the split-screen layout renders correctly at ≥768px and collapses to single-panel at <768px. Ask the user if questions arise.

- [ ] 38. Add Flyway migration and backend DTO updates for book cover URL (F3)
  - Create `backend/src/main/resources/db/migration/V7__add_cover_url_to_books.sql` — add optional `cover_url VARCHAR(2048)` column to the `books` table with `ALTER TABLE books ADD COLUMN cover_url VARCHAR(2048)`
  - Update `backend/.../catalog/api/dto/BookRequest.java` to include optional `coverUrl` field (nullable `String`)
  - Update `backend/.../catalog/api/dto/BookResponse.java` to include optional `coverUrl` field
  - Update `backend/.../catalog/application/dto/BookRequest.java` and `BookResponse.java` (application layer DTOs) to include `coverUrl`
  - Update `backend/.../catalog/domain/Book.java` entity to include `coverUrl` field (`@Column(name = "cover_url", length = 2048)`, nullable)
  - Update `backend/.../catalog/application/BookServiceImpl.java` to map `coverUrl` from request to entity on create/update, and include it in the response
  - _Requirements: 15.7_

- [ ] 39. Implement bookCoverService (F3)
  - [ ] 39.1 Create `src/services/bookCoverService.ts` with two exported async functions:
    - `fetchCoverByISBN(isbn: string): Promise<string | null>` — calls Open Library Covers API (`https://covers.openlibrary.org/b/isbn/{ISBN}-L.jpg`) with a HEAD request to validate the image is not a placeholder (Content-Length < 1000 bytes → return null); catches all errors and returns null
    - `fetchCoverByTitleAuthor(title: string, author: string): Promise<string | null>` — searches `https://openlibrary.org/search.json?title=...&author=...&limit=1&fields=isbn`, extracts the first ISBN, delegates to `fetchCoverByISBN`; catches all errors and returns null
    - _Requirements: 15.9, 15.10, 15.11_
  - [ ]* 39.2 Write Vitest property tests for `bookCoverService`
    - **Property 30: bookCoverService returns null on network error, never throws**
    - Mock `fetch` to simulate network failure, HTTP 4xx, HTTP 5xx, and malformed JSON responses
    - Assert both functions return `null` and do not throw for all error scenarios
    - **Validates: Requirements 15.11**

- [ ] 40. Implement BookCoverFetcher component (F3)
  - [ ] 40.1 Create `src/components/BookCoverFetcher.tsx` with props `isbn?`, `title?`, `author?`, `onCoverFound(url: string)` and internal state `{ loading, coverUrl, error }`
    - On ISBN `onBlur` trigger: calls `fetchCoverByISBN(isbn)` automatically
    - "Search by title/author" button (shown when no ISBN): calls `fetchCoverByTitleAuthor(title, author)`
    - Loading state: renders a spinner/skeleton in the 200×300px preview area
    - Cover found: renders `<img>` at 200×300px with `object-fit: cover`, `border-radius: 8px`, calls `onCoverFound(url)`
    - Cover not found / error: renders fallback placeholder with book icon and `t('cover.notFound')` text
    - _Requirements: 15.3, 15.4, 15.5, 15.8_
  - [ ]* 40.2 Write Vitest unit tests for `BookCoverFetcher`
    - **Property 31: BookCoverFetcher shows fallback when no cover found**
    - Test: when `bookCoverService` returns `null`, the fallback placeholder is rendered and no `<img>` element is present
    - Test: when `bookCoverService` returns a URL, the `<img>` is rendered and `onCoverFound` is called with the URL
    - Test: loading spinner is shown while the service call is in progress
    - **Validates: Requirements 15.5, 15.13**
  - [ ] 40.3 Update `src/pages/admin/AdminBookFormPage.tsx` to include `<BookCoverFetcher>` with `onBlur` trigger on the ISBN field
    - Wire `onCoverFound` to update the form state's `coverUrl` field
    - Include the `coverUrl` in the payload sent to `POST /books` or `PUT /books/:id`
    - _Requirements: 15.1, 15.2, 15.6, 15.7_

- [ ] 41. Update TypeScript types and BookCard for cover display (F3)
  - [ ] 41.1 Update `src/types/index.ts` — add `coverUrl?: string | null` to the `Book` interface and `coverUrl?: string | null` to the `BookRequest` interface
    - _Requirements: 15.12, 15.13_
  - [ ] 41.2 Update `src/components/BookCard.tsx` to display a cover thumbnail at 80×120px with `object-fit: cover` when `book.coverUrl` is available
    - When `coverUrl` is null or undefined, render a dark gray (`#2a2a2a`) fallback div with a centered book icon
    - _Requirements: 15.12, 15.13_
  - [ ]* 41.3 Write Vitest unit tests for the updated `BookCard`
    - Test: renders cover `<img>` at 80×120px when `coverUrl` is a non-empty string
    - Test: renders dark gray fallback div (no `<img>`) when `coverUrl` is null
    - Test: renders dark gray fallback div (no `<img>`) when `coverUrl` is undefined
    - _Requirements: 15.12, 15.13_

- [ ] 42. Final Phase 4 checkpoint
  - Ensure all Vitest tests pass. Verify the cover fetcher triggers on ISBN blur in the admin book form. Verify BookCard renders cover thumbnails and fallbacks correctly. Verify language switching updates all UI text including the login quote. Ask the user if questions arise.

---

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP
- Property tests use jqwik `@Property(tries = 100)` with generators as described in the design's Testing Strategy section
- Each property test must include the tag comment: `// Feature: personal-library-manager, Property N: <property text>`
- Integration tests use `@QuarkusTest` + Testcontainers; the PostgreSQL container is started once per test suite via a shared `@QuarkusTestResource`
- Frontend MSW handlers should mirror the exact API contracts defined in requirements.md
- JaCoCo coverage is enforced at the Maven `verify` phase; the build fails if coverage drops below 80% on service and domain classes
