# Requirements Document

## Introduction

The Personal Library Manager is a web application that allows users to track their reading activity. Administrators manage the book catalog (books, authors, publishers). Authenticated users create and manage reading records, including status, ratings, and reviews. The architecture is designed to support a future AI-powered recommendation service without coupling it to core business logic.

The system is built as a modular monolith in its first iteration, with clear service boundaries that allow extraction into microservices later. Messaging events are emitted for key domain actions to support future integrations (e.g., the recommendation service).

---

## Glossary

- **System**: The Personal Library Manager application as a whole.
- **Identity_Service**: The module responsible for user registration, authentication, and role management.
- **Catalog_Service**: The module responsible for managing books, authors, and publishers.
- **Reading_Service**: The module responsible for managing user reading records, ratings, and reviews.
- **Recommendation_Service**: A future module that will consume events and call an LLM to suggest books. Not implemented in the first version.
- **Event_Bus**: The internal messaging infrastructure (e.g., backed by a message broker) used to publish and consume domain events.
- **User**: A registered person with role USER or ADMIN.
- **Admin**: A User with role ADMIN who has elevated privileges over catalog management.
- **Profile**: Extended personal information associated with a User.
- **Book**: A catalog entry representing a published work, identified by ISBN.
- **Author**: A person who authored one or more books.
- **Publisher**: An organization that published one or more books.
- **Reading**: A record associating a User with a Book, including status, optional rating, and optional review.
- **JWT**: JSON Web Token used for stateless authentication.
- **DTO**: Data Transfer Object used to decouple API input/output from domain entities.
- **Repository**: A persistence abstraction over the database layer.
- **Mapper**: A component that converts between domain entities and DTOs.
- **i18n**: Internationalization — the process of designing the application to support multiple languages.
- **Locale**: A language/region code such as `pt-BR` (Brazilian Portuguese) or `en-US` (American English).
- **LanguageSwitcher**: A UI component that allows the user to toggle between supported locales.
- **BookCoverFetcher**: A UI component that searches for and previews a book cover image based on ISBN, title, or author.
- **bookCoverService**: A frontend service module that calls external cover image APIs (Open Library, Google Books) and returns a cover URL or null.
- **getLanguageInstruction**: A utility function that returns the appropriate language instruction string for AI API prompts based on the active locale.

---

## Requirements

### Requirement 1: User Registration

**User Story:** As a visitor, I want to register an account, so that I can access the library manager.

#### Acceptance Criteria

1. WHEN a registration request is received with a valid name, email, and password, THE Identity_Service SHALL create a new User with role USER and return a 201 response.
2. WHEN a registration request is received with an email that already exists, THE Identity_Service SHALL return a 409 Conflict response with a descriptive error message.
3. WHEN a registration request is received with a missing or malformed email, THE Identity_Service SHALL return a 400 Bad Request response listing the validation errors.
4. WHEN a registration request is received with a password shorter than 8 characters, THE Identity_Service SHALL return a 400 Bad Request response.
5. THE Identity_Service SHALL store the password as a bcrypt hash and SHALL NOT store the plaintext password.
6. WHEN a User is successfully created, THE Identity_Service SHALL publish a `user.created` event to the Event_Bus.

---

### Requirement 2: Authentication

**User Story:** As a registered user, I want to log in with my credentials, so that I can receive a token to access protected resources.

#### Acceptance Criteria

1. WHEN a login request is received with a valid email and correct password, THE Identity_Service SHALL return a signed JWT containing the user ID, email, and role, with an expiry of 1 hour.
2. WHEN a login request is received with a valid email and incorrect password, THE Identity_Service SHALL return a 401 Unauthorized response.
3. WHEN a login request is received with an email that does not exist, THE Identity_Service SHALL return a 401 Unauthorized response.
4. WHEN a request is received on a protected endpoint without a JWT, THE System SHALL return a 401 Unauthorized response.
5. WHEN a request is received on a protected endpoint with an expired or invalid JWT, THE System SHALL return a 401 Unauthorized response.
6. THE Identity_Service SHALL sign JWTs using RS256 and a private key stored in application configuration.

---

### Requirement 3: Role-Based Authorization

**User Story:** As a system designer, I want role-based access control, so that only authorized users can perform privileged operations.

#### Acceptance Criteria

1. WHEN a request to create, update, or delete a Book is received from a User with role USER, THE System SHALL return a 403 Forbidden response.
2. WHEN a request to create, update, or delete an Author is received from a User with role USER, THE System SHALL return a 403 Forbidden response.
3. WHEN a request to create, update, or delete a Publisher is received from a User with role USER, THE System SHALL return a 403 Forbidden response.
4. WHEN a request to create, update, or delete a Book is received from a User with role ADMIN, THE System SHALL process the request.
5. THE System SHALL extract the user role from the JWT claims on every protected request.
6. WHERE role promotion is needed, THE Identity_Service SHALL expose an admin-only endpoint to update a User's role.

---

### Requirement 4: User Profile Management

**User Story:** As a registered user, I want to manage my profile, so that I can personalize my presence in the system.

#### Acceptance Criteria

1. WHEN a User is created, THE Identity_Service SHALL automatically create an associated Profile with empty displayName, bio, and favoriteGenres.
2. WHEN an authenticated User sends a request to update their Profile, THE Identity_Service SHALL update the displayName, bio, and favoriteGenres fields and return the updated Profile.
3. WHEN an authenticated User requests their own Profile, THE Identity_Service SHALL return the Profile data.
4. WHEN a User requests a Profile that belongs to another User, THE Identity_Service SHALL return a 403 Forbidden response.
5. IF a Profile update request contains a displayName longer than 100 characters, THEN THE Identity_Service SHALL return a 400 Bad Request response.
6. IF a Profile update request contains a bio longer than 1000 characters, THEN THE Identity_Service SHALL return a 400 Bad Request response.

---

### Requirement 5: Author Management

**User Story:** As an administrator, I want to manage authors, so that books can be associated with their creators.

#### Acceptance Criteria

1. WHEN an Admin sends a request to create an Author with a valid name, THE Catalog_Service SHALL persist the Author and return a 201 response with the created Author.
2. WHEN an Admin sends a request to update an Author, THE Catalog_Service SHALL update the Author's name and return the updated Author.
3. WHEN an Admin sends a request to delete an Author that has no associated Books, THE Catalog_Service SHALL delete the Author and return a 204 response.
4. IF an Admin sends a request to delete an Author that has one or more associated Books, THEN THE Catalog_Service SHALL return a 409 Conflict response.
5. WHEN any authenticated User requests the list of Authors, THE Catalog_Service SHALL return a paginated list of Authors.
6. WHEN any authenticated User requests an Author by ID, THE Catalog_Service SHALL return the Author or a 404 Not Found response if the Author does not exist.
7. WHEN a create or update request is received with a missing or blank name, THE Catalog_Service SHALL return a 400 Bad Request response.

---

### Requirement 6: Publisher Management

**User Story:** As an administrator, I want to manage publishers, so that books can be associated with their publishing organizations.

#### Acceptance Criteria

1. WHEN an Admin sends a request to create a Publisher with a valid name, THE Catalog_Service SHALL persist the Publisher and return a 201 response with the created Publisher.
2. WHEN an Admin sends a request to update a Publisher, THE Catalog_Service SHALL update the Publisher's name and return the updated Publisher.
3. WHEN an Admin sends a request to delete a Publisher that has no associated Books, THE Catalog_Service SHALL delete the Publisher and return a 204 response.
4. IF an Admin sends a request to delete a Publisher that has one or more associated Books, THEN THE Catalog_Service SHALL return a 409 Conflict response.
5. WHEN any authenticated User requests the list of Publishers, THE Catalog_Service SHALL return a paginated list of Publishers.
6. WHEN any authenticated User requests a Publisher by ID, THE Catalog_Service SHALL return the Publisher or a 404 Not Found response if the Publisher does not exist.
7. WHEN a create or update request is received with a missing or blank name, THE Catalog_Service SHALL return a 400 Bad Request response.

---

### Requirement 7: Book Catalog Management

**User Story:** As an administrator, I want to manage the book catalog, so that users have an accurate and up-to-date list of books to track.

#### Acceptance Criteria

1. WHEN an Admin sends a request to create a Book with a valid ISBN, title, authorId, and publisherId, THE Catalog_Service SHALL persist the Book and return a 201 response with the created Book.
2. WHEN an Admin sends a request to create a Book with an ISBN that already exists, THE Catalog_Service SHALL return a 409 Conflict response.
3. WHEN an Admin sends a request to update a Book, THE Catalog_Service SHALL update the allowed fields (title, authorId, publisherId) and return the updated Book.
4. WHEN an Admin sends a request to delete a Book, THE Catalog_Service SHALL delete the Book and return a 204 response.
5. WHEN any authenticated User requests the list of Books, THE Catalog_Service SHALL return a paginated list of Books including title, author name, publisher name, and ISBN.
6. WHEN any authenticated User requests a Book by ID, THE Catalog_Service SHALL return the full Book details or a 404 Not Found response if the Book does not exist.
7. WHEN a Book creation request references an authorId that does not exist, THE Catalog_Service SHALL return a 422 Unprocessable Entity response.
8. WHEN a Book creation request references a publisherId that does not exist, THE Catalog_Service SHALL return a 422 Unprocessable Entity response.
9. WHEN a Book is successfully created, THE Catalog_Service SHALL publish a `book.created` event to the Event_Bus.
10. WHEN a Book is successfully updated, THE Catalog_Service SHALL publish a `book.updated` event to the Event_Bus.
11. WHEN a Book is successfully deleted, THE Catalog_Service SHALL publish a `book.deleted` event to the Event_Bus.
12. WHEN a Book list request includes a search query parameter, THE Catalog_Service SHALL return only Books whose title or author name contains the query string (case-insensitive).

---

### Requirement 8: Reading Record Management

**User Story:** As a registered user, I want to create and manage reading records, so that I can track my reading progress, ratings, and reviews.

#### Acceptance Criteria

1. WHEN an authenticated User sends a request to create a Reading for a Book, THE Reading_Service SHALL create the Reading with status WANT_TO_READ and return a 201 response.
2. WHEN an authenticated User sends a request to create a Reading for a Book that the User already has a Reading record for, THE Reading_Service SHALL return a 409 Conflict response.
3. WHEN an authenticated User sends a request to update their own Reading, THE Reading_Service SHALL update the status, rating, review, startedAt, and finishedAt fields and return the updated Reading.
4. WHEN an Admin sends a request to update any Reading, THE Reading_Service SHALL process the update.
5. WHEN a User sends a request to update a Reading that belongs to another User, THE Reading_Service SHALL return a 403 Forbidden response.
6. WHEN an authenticated User requests their own list of Readings, THE Reading_Service SHALL return a paginated list of the User's Readings including Book title and author.
7. WHEN an authenticated User requests a Reading by ID that belongs to them, THE Reading_Service SHALL return the Reading details.
8. WHEN a User requests a Reading by ID that belongs to another User, THE Reading_Service SHALL return a 403 Forbidden response.
9. IF a Reading update request contains a rating outside the range 0.0 to 10.0, THEN THE Reading_Service SHALL return a 400 Bad Request response.
10. IF a Reading update request sets status to FINISHED and startedAt is not set, THEN THE Reading_Service SHALL return a 400 Bad Request response.
11. WHEN a Reading is successfully created, THE Reading_Service SHALL publish a `reading.created` event to the Event_Bus.
12. WHEN a Reading rating is updated, THE Reading_Service SHALL publish a `rating.updated` event to the Event_Bus.
13. WHEN a Reading review is submitted or updated, THE Reading_Service SHALL publish a `review.submitted` event to the Event_Bus.
14. WHEN an authenticated User sends a request to delete their own Reading, THE Reading_Service SHALL delete the Reading and return a 204 response.

---

### Requirement 9: Messaging and Domain Events

**User Story:** As a system architect, I want domain events published to a message bus, so that future services (such as the Recommendation_Service) can react to changes without coupling to core modules.

#### Acceptance Criteria

1. THE Event_Bus SHALL support at-least-once delivery for all domain events.
2. WHEN a `user.created` event is published, THE Event_Bus SHALL include the userId, email, and createdAt timestamp in the event payload.
3. WHEN a `book.created` event is published, THE Event_Bus SHALL include the bookId, isbn, title, authorId, and publisherId in the event payload.
4. WHEN a `book.updated` event is published, THE Event_Bus SHALL include the bookId and the updated fields in the event payload.
5. WHEN a `book.deleted` event is published, THE Event_Bus SHALL include the bookId in the event payload.
6. WHEN a `reading.created` event is published, THE Event_Bus SHALL include the readingId, userId, bookId, and status in the event payload.
7. WHEN a `rating.updated` event is published, THE Event_Bus SHALL include the readingId, userId, bookId, and the new rating value in the event payload.
8. WHEN a `review.submitted` event is published, THE Event_Bus SHALL include the readingId, userId, bookId, and the review text in the event payload.
9. THE System SHALL serialize all event payloads as JSON.
10. WHERE a message broker is not available in the development environment, THE System SHALL support an in-process event bus fallback for local development.

---

### Requirement 10: Future Recommendation Service (Architecture Constraint)

**User Story:** As a product owner, I want the architecture to support a future LLM-based recommendation service, so that book suggestions can be added without modifying existing modules.

#### Acceptance Criteria

1. THE Reading_Service SHALL publish `rating.updated` and `review.submitted` events without any direct dependency on the Recommendation_Service.
2. THE Recommendation_Service SHALL consume events from the Event_Bus and SHALL NOT call Reading_Service or Catalog_Service APIs directly for recommendation logic.
3. THE System SHALL define a `RecommendationRequest` and `RecommendationResponse` contract in a shared API module so the Recommendation_Service can be integrated later without breaking changes.
4. WHERE the Recommendation_Service is not deployed, THE System SHALL return an empty recommendations list from the recommendations endpoint rather than an error.
5. THE Catalog_Service and Reading_Service SHALL NOT contain any import or compile-time dependency on Recommendation_Service classes.

---

### Requirement 11: Pagination and Filtering

**User Story:** As a user, I want list endpoints to support pagination and filtering, so that I can navigate large datasets efficiently.

#### Acceptance Criteria

1. THE System SHALL support `page` and `size` query parameters on all list endpoints, defaulting to page 0 and size 20.
2. IF a `size` parameter greater than 100 is provided, THEN THE System SHALL return a 400 Bad Request response.
3. THE System SHALL include `totalElements`, `totalPages`, `page`, and `size` in all paginated responses.
4. WHEN a `search` query parameter is provided on the Books list endpoint, THE Catalog_Service SHALL filter results by title or author name using a case-insensitive partial match.
5. WHEN a `status` query parameter is provided on the Readings list endpoint, THE Reading_Service SHALL filter results by the given Reading status.

---

### Requirement 12: Error Handling and Observability

**User Story:** As a developer, I want consistent error responses and structured logging, so that I can diagnose issues quickly.

#### Acceptance Criteria

1. THE System SHALL return all error responses in a consistent JSON structure containing `status`, `message`, and `timestamp` fields.
2. WHEN an unhandled exception occurs, THE System SHALL return a 500 Internal Server Error response without exposing internal stack traces to the client.
3. THE System SHALL log all unhandled exceptions at ERROR level with a correlation ID.
4. WHEN a request is received, THE System SHALL assign a correlation ID and include it in all log entries for that request.
5. THE System SHALL expose a health check endpoint at `GET /q/health` that returns the status of the application and its database connection.

---

---

### Requirement 13: Internationalization — Language Switcher (PT-BR / EN-US)

**User Story:** As a user, I want to switch the application language between Portuguese (PT-BR) and English (EN-US), so that I can use the interface in my preferred language.

#### Acceptance Criteria

1. THE Frontend SHALL support two locales: `pt-BR` (default) and `en-US`, implemented using `react-i18next` and `i18next`.
2. WHEN the application loads for the first time, THE Frontend SHALL display the interface in `pt-BR` unless a previously saved language preference exists in `localStorage`.
3. WHEN a User selects a different language via the language switcher, THE Frontend SHALL update all visible UI text immediately without reloading the page.
4. WHEN a User selects a language, THE Frontend SHALL persist the selected language code in `localStorage` under the key `i18n_language` so that the preference is restored on the next session.
5. THE Frontend SHALL expose the active language and a language-change function via the `useTranslation()` hook from `react-i18next`, making it available to all components.
6. THE Frontend SHALL translate all static UI text, including: form labels and placeholders, page and section titles, button labels (Save, Cancel, Edit, Delete, etc.), error and validation messages, empty-state messages, navigation menu items, and tooltips.
7. THE Frontend SHALL include a `LanguageSwitcher` component rendered in the Navbar, positioned on the right side before any avatar or profile element.
8. THE LanguageSwitcher SHALL display as a compact inline toggle showing "PT | EN" with a separator, with the active language highlighted in the brand orange color (`#E07020`).
9. THE LanguageSwitcher SHALL toggle directly between the two languages on click, without displaying a dropdown menu.
10. THE Frontend SHALL store all Portuguese strings in `src/i18n/locales/pt-BR.json` and all English strings in `src/i18n/locales/en-US.json`.
11. THE Frontend SHALL configure `i18next` in `src/i18n/index.ts`, initializing with `localStorage` language detection and fallback to `pt-BR`.

---

### Requirement 14: Recommendations in the Active Language

**User Story:** As a user, I want book recommendations to be delivered in the language I have selected, so that the recommendation content is consistent with the rest of the interface.

#### Acceptance Criteria

1. WHEN the Recommendation_Service generates recommendations via an external AI API (e.g., OpenAI, Claude, Gemini), THE Frontend SHALL include a language instruction in the prompt sent to the API, derived from the currently active `i18next` locale.
2. THE Frontend SHALL provide a utility function `getLanguageInstruction(locale: string): string` in `src/utils/getLanguageInstruction.ts` that returns `"Responda em português do Brasil."` when the locale is `pt-BR` and `"Reply in English (US)."` when the locale is `en-US`.
3. WHEN the active language is `pt-BR`, THE Frontend SHALL append the instruction `"Responda em português do Brasil."` to any prompt sent to the AI recommendation API.
4. WHEN the active language is `en-US`, THE Frontend SHALL append the instruction `"Reply in English (US)."` to any prompt sent to the AI recommendation API.
5. WHERE recommendations are generated from rule-based or local data rather than an AI API, THE Frontend SHALL use `i18next` translation keys for all recommendation text, with corresponding entries in `pt-BR.json` and `en-US.json`.
6. WHEN a User switches language, THE Frontend SHALL ensure that newly fetched recommendations are requested in the newly active language.
7. WHERE the recommendation display includes a language badge, THE Frontend SHALL show `"Gerado em PT"` or `"Generated in EN"` to indicate the language in which the recommendation was generated.

---

### Requirement 15: Automatic Book Cover Search

**User Story:** As a user registering or editing a book, I want the system to automatically search for and display the book's cover image, so that my library has visual representations of each book.

#### Acceptance Criteria

1. WHEN a User fills in the ISBN field in the book registration or edit form and the field loses focus (`onBlur`), THE Frontend SHALL automatically call the cover search service using the ISBN as the primary lookup key.
2. WHEN a User fills in the Title and Author fields and the ISBN is not available, THE Frontend SHALL allow triggering a cover search using the title and author as fallback lookup keys.
3. WHILE a cover search is in progress, THE Frontend SHALL display a loading spinner or skeleton in the cover preview area.
4. WHEN a cover image is found, THE Frontend SHALL display a preview of the image at 200×300px with `object-fit: cover`, a subtle border, and `border-radius: 8px`.
5. IF a cover search returns no result or encounters a network error, THEN THE Frontend SHALL display a placeholder with a book icon and the text `"Capa não encontrada"` (PT-BR) or `"Cover not found"` (EN-US), respecting the active language.
6. WHEN a cover is found and displayed, THE User SHALL be able to accept the found cover or manually upload a different image.
7. WHEN a book is saved, THE Frontend SHALL include the cover image URL in the book data sent to the backend.
8. THE Frontend SHALL implement a `BookCoverFetcher` component in `src/components/BookCoverFetcher.tsx` with props `isbn?`, `title?`, `author?`, and `onCoverFound(url: string)`, and internal state for `loading`, `coverUrl`, and `error`.
9. THE Frontend SHALL implement a `bookCoverService` in `src/services/bookCoverService.ts` exposing `fetchCoverByISBN(isbn: string): Promise<string | null>` and `fetchCoverByTitleAuthor(title: string, author: string): Promise<string | null>`.
10. THE bookCoverService SHALL use the Open Library Covers API (`https://covers.openlibrary.org/b/isbn/{ISBN}-L.jpg`) as the primary source, with a fallback search via `https://openlibrary.org/search.json?title={title}&author={author}`.
11. IF a network error occurs during cover retrieval, THEN THE bookCoverService SHALL return `null` and SHALL NOT throw an unhandled exception or break the form.
12. WHEN displaying a book in any list or card view, THE Frontend SHALL show the cover image if available, at 80×120px with `object-fit: cover`.
13. IF a book has no cover image, THEN THE Frontend SHALL display a dark gray background with a centered book icon as a visual fallback in all card and list views.

---

### Requirement 16: Thematic Background on the Login Screen

**User Story:** As a user, I want the login screen to have a visually appealing library-themed background, so that the application makes a strong first impression.

#### Acceptance Criteria

1. THE Login screen SHALL be divided into two panels: a left panel (60% width) containing a background image and an inspirational quote, and a right panel (40% width) containing the login form on a dark background.
2. THE left panel SHALL display a library or bookshelf background image with a semi-transparent dark overlay using `linear-gradient(135deg, rgba(0,0,0,0.7), rgba(224,112,32,0.15))` to maintain readability and reinforce brand identity.
3. THE left panel SHALL display an inspirational reading quote in italic, serif font, white color at 0.85 opacity, and 18px font size.
4. WHEN the active language is `pt-BR`, THE left panel SHALL display the quote: `"Um leitor vive mil vidas antes de morrer. Quem nunca lê, vive apenas uma." — George R.R. Martin`.
5. WHEN the active language is `en-US`, THE left panel SHALL display the quote: `"A reader lives a thousand lives before he dies. The man who never reads lives only one." — George R.R. Martin`.
6. THE right panel SHALL display the GMLib logo centered above the login form.
7. THE background image SHALL use `object-fit: cover` and `object-position: center` to fill the left panel at all viewport sizes.
8. WHILE the viewport width is less than 768px, THE Frontend SHALL hide the left panel and display only the login form, using the background image as a blurred full-screen background behind the form.
9. IF the external background image URL is unavailable, THEN THE Frontend SHALL fall back to a solid dark background color so that the login form remains fully functional and readable.
10. THE Login screen layout SHALL NOT break or obscure the existing login form fields, validation messages, or submit button.

---

## Non-Functional Requirements

### NFR 1: Security

1. THE System SHALL enforce HTTPS for all API endpoints in production.
2. THE Identity_Service SHALL hash passwords using bcrypt with a minimum cost factor of 12.
3. THE System SHALL validate and sanitize all user-supplied input before persistence.
4. THE System SHALL not include sensitive fields (passwordHash, internal IDs of other users) in API responses.

### NFR 2: Performance

1. THE System SHALL respond to read requests (GET) within 500ms at the 95th percentile under normal load.
2. THE System SHALL respond to write requests (POST, PUT, DELETE) within 1000ms at the 95th percentile under normal load.

### NFR 3: Maintainability

1. THE System SHALL separate domain, application, infrastructure, and API layers in the codebase.
2. THE System SHALL use DTOs for all API input and output and SHALL NOT expose JPA entities directly in REST responses.
3. THE System SHALL use Mappers to convert between entities and DTOs.
4. THE System SHALL use Repository interfaces for all database access.

### NFR 4: Testability

1. THE System SHALL include unit tests for all business rule validations.
2. THE System SHALL include integration tests for all REST endpoints.
3. THE System SHALL achieve a minimum of 80% line coverage on service and domain classes.

---

## Domain Model

### Entities

| Entity    | Fields |
|-----------|--------|
| User      | id (UUID), name, email (unique), passwordHash, role (ADMIN\|USER), createdAt, updatedAt |
| Profile   | id (UUID), userId (FK → User, unique), displayName (max 100), bio (max 1000), favoriteGenres (text array) |
| Author    | id (UUID), name, createdAt, updatedAt |
| Publisher | id (UUID), name, createdAt, updatedAt |
| Book      | id (UUID), isbn (unique, max 13), title, authorId (FK → Author), publisherId (FK → Publisher), createdAt, updatedAt |
| Reading   | id (UUID), userId (FK → User), bookId (FK → Book), status (WANT_TO_READ\|READING\|FINISHED\|ABANDONED), rating (decimal 0.0–10.0, nullable), review (text, nullable), startedAt (nullable), finishedAt (nullable), createdAt, updatedAt |

### Constraints

- `(userId, bookId)` on Reading is UNIQUE (no duplicate reading records per user per book).
- `rating` must be between 0.0 and 10.0 inclusive when provided.
- `finishedAt` must be after `startedAt` when both are provided.

---

## Database Migration Strategy

- Use Flyway for versioned SQL migrations.
- Migration scripts live in `src/main/resources/db/migration/`.
- Naming convention: `V{version}__{description}.sql` (e.g., `V1__create_users.sql`).
- Each entity gets its own migration script.
- Migration order: users → profiles → authors → publishers → books → readings.

---

## API Contracts

### Identity Service — `/api/v1`

| Method | Endpoint | Auth | Role | Description |
|--------|----------|------|------|-------------|
| POST | `/auth/register` | No | — | Register a new user |
| POST | `/auth/login` | No | — | Authenticate and receive JWT |
| GET | `/users/me` | JWT | USER, ADMIN | Get current user info |
| PUT | `/users/me/profile` | JWT | USER, ADMIN | Update own profile |
| GET | `/users/me/profile` | JWT | USER, ADMIN | Get own profile |
| PUT | `/users/{id}/role` | JWT | ADMIN | Update a user's role |

#### POST /auth/register
Request:
```json
{ "name": "string", "email": "string", "password": "string (min 8 chars)" }
```
Response 201:
```json
{ "id": "uuid", "name": "string", "email": "string", "role": "USER", "createdAt": "ISO-8601" }
```

#### POST /auth/login
Request:
```json
{ "email": "string", "password": "string" }
```
Response 200:
```json
{ "token": "string (JWT)", "expiresIn": 3600 }
```

---

### Catalog Service — `/api/v1`

| Method | Endpoint | Auth | Role | Description |
|--------|----------|------|------|-------------|
| POST | `/authors` | JWT | ADMIN | Create author |
| GET | `/authors` | JWT | USER, ADMIN | List authors (paginated) |
| GET | `/authors/{id}` | JWT | USER, ADMIN | Get author by ID |
| PUT | `/authors/{id}` | JWT | ADMIN | Update author |
| DELETE | `/authors/{id}` | JWT | ADMIN | Delete author |
| POST | `/publishers` | JWT | ADMIN | Create publisher |
| GET | `/publishers` | JWT | USER, ADMIN | List publishers (paginated) |
| GET | `/publishers/{id}` | JWT | USER, ADMIN | Get publisher by ID |
| PUT | `/publishers/{id}` | JWT | ADMIN | Update publisher |
| DELETE | `/publishers/{id}` | JWT | ADMIN | Delete publisher |
| POST | `/books` | JWT | ADMIN | Create book |
| GET | `/books` | JWT | USER, ADMIN | List books (paginated, searchable) |
| GET | `/books/{id}` | JWT | USER, ADMIN | Get book by ID |
| PUT | `/books/{id}` | JWT | ADMIN | Update book |
| DELETE | `/books/{id}` | JWT | ADMIN | Delete book |

#### POST /books
Request:
```json
{ "isbn": "string (max 13)", "title": "string", "authorId": "uuid", "publisherId": "uuid" }
```
Response 201:
```json
{ "id": "uuid", "isbn": "string", "title": "string", "author": { "id": "uuid", "name": "string" }, "publisher": { "id": "uuid", "name": "string" }, "createdAt": "ISO-8601" }
```

#### GET /books
Query params: `page`, `size`, `search`
Response 200:
```json
{ "content": [ { "id": "uuid", "isbn": "string", "title": "string", "author": { "id": "uuid", "name": "string" }, "publisher": { "id": "uuid", "name": "string" } } ], "totalElements": 0, "totalPages": 0, "page": 0, "size": 20 }
```

---

### Reading Service — `/api/v1`

| Method | Endpoint | Auth | Role | Description |
|--------|----------|------|------|-------------|
| POST | `/readings` | JWT | USER, ADMIN | Create reading record |
| GET | `/readings` | JWT | USER, ADMIN | List own readings (paginated) |
| GET | `/readings/{id}` | JWT | USER, ADMIN | Get reading by ID (own only) |
| PUT | `/readings/{id}` | JWT | USER, ADMIN | Update reading (own or ADMIN) |
| DELETE | `/readings/{id}` | JWT | USER, ADMIN | Delete reading (own or ADMIN) |

#### POST /readings
Request:
```json
{ "bookId": "uuid" }
```
Response 201:
```json
{ "id": "uuid", "book": { "id": "uuid", "title": "string" }, "status": "WANT_TO_READ", "rating": null, "review": null, "startedAt": null, "finishedAt": null, "createdAt": "ISO-8601" }
```

#### PUT /readings/{id}
Request:
```json
{ "status": "READING|FINISHED|ABANDONED|WANT_TO_READ", "rating": 0.0, "review": "string", "startedAt": "ISO-8601", "finishedAt": "ISO-8601" }
```

---

### Recommendation Service — `/api/v1` (Future)

| Method | Endpoint | Auth | Role | Description |
|--------|----------|------|------|-------------|
| GET | `/recommendations` | JWT | USER, ADMIN | Get book recommendations for current user |

Response 200 (stub):
```json
{ "recommendations": [] }
```

---

## Event Contracts

All events are JSON-serialized and published to the Event_Bus.

| Event | Topic | Payload Fields |
|-------|-------|----------------|
| `user.created` | `user-events` | userId, email, createdAt |
| `book.created` | `book-events` | bookId, isbn, title, authorId, publisherId |
| `book.updated` | `book-events` | bookId, updatedFields (map) |
| `book.deleted` | `book-events` | bookId |
| `reading.created` | `reading-events` | readingId, userId, bookId, status |
| `rating.updated` | `reading-events` | readingId, userId, bookId, rating |
| `review.submitted` | `reading-events` | readingId, userId, bookId, review |

### Event Envelope (all events)
```json
{
  "eventId": "uuid",
  "eventType": "string",
  "occurredAt": "ISO-8601",
  "payload": { }
}
```

---

## Frontend Route Map

| Route | Component | Auth Required | Description |
|-------|-----------|---------------|-------------|
| `/login` | `LoginPage` | No | Login form |
| `/register` | `RegisterPage` | No | Registration form |
| `/profile` | `ProfilePage` | Yes | View and edit own profile |
| `/books` | `BookListPage` | Yes | Paginated, searchable book list |
| `/books/:id` | `BookDetailPage` | Yes | Book details with reading action |
| `/admin/books/new` | `AdminBookFormPage` | Yes (ADMIN) | Create book form |
| `/admin/books/:id/edit` | `AdminBookFormPage` | Yes (ADMIN) | Edit book form |
| `/admin/authors` | `AdminAuthorListPage` | Yes (ADMIN) | Author management list |
| `/admin/authors/new` | `AdminAuthorFormPage` | Yes (ADMIN) | Create author form |
| `/admin/authors/:id/edit` | `AdminAuthorFormPage` | Yes (ADMIN) | Edit author form |
| `/admin/publishers` | `AdminPublisherListPage` | Yes (ADMIN) | Publisher management list |
| `/admin/publishers/new` | `AdminPublisherFormPage` | Yes (ADMIN) | Create publisher form |
| `/admin/publishers/:id/edit` | `AdminPublisherFormPage` | Yes (ADMIN) | Edit publisher form |
| `/readings` | `MyReadingsPage` | Yes | User's reading list with filters |
| `/readings/:id` | `ReadingDetailPage` | Yes | Reading detail with review/rating form |
| `/recommendations` | `RecommendationsPage` | Yes | AI-generated book recommendations |

### New Frontend Components (F1–F4)

| Component / File | Description |
|---|---|
| `src/i18n/index.ts` | i18next configuration with localStorage language detection and pt-BR fallback |
| `src/i18n/locales/pt-BR.json` | All UI strings in Brazilian Portuguese |
| `src/i18n/locales/en-US.json` | All UI strings in American English |
| `src/components/LanguageSwitcher.tsx` | Inline "PT \| EN" toggle rendered in the Navbar |
| `src/utils/getLanguageInstruction.ts` | Returns AI prompt language instruction based on active locale |
| `src/components/BookCoverFetcher.tsx` | Cover preview component with loading/error states |
| `src/services/bookCoverService.ts` | Open Library / Google Books cover fetch service |

---

## Architecture Decision: Modular Monolith First

### Recommendation

Start as a modular monolith with clear internal module boundaries. Extract to microservices only when there is a demonstrated operational need (independent scaling, team autonomy, deployment frequency).

### Trade-offs

| Concern | Modular Monolith | Microservices |
|---------|-----------------|---------------|
| Operational complexity | Low — single deployment | High — multiple services, service discovery, distributed tracing |
| Development speed | High — no network overhead, shared DB transactions | Lower — inter-service calls, eventual consistency |
| Scalability | Vertical + limited horizontal | Full horizontal per service |
| Future extraction | Possible with clean boundaries | Already extracted |
| First iteration risk | Low | High |

### Module Boundaries (within the monolith)

```
com.library
├── identity        # User, Profile, Auth, JWT
├── catalog         # Book, Author, Publisher
├── reading         # Reading, Rating, Review
├── recommendation  # Stub only — event consumer interface
└── shared          # Event contracts, DTOs, common utilities
```

Each module owns its own:
- Domain entities
- Repository interfaces
- Service/use-case classes
- REST resource classes
- DTOs and Mappers
- Database migration scripts

Modules communicate only through:
1. Published domain events (via Event_Bus)
2. Explicit public API interfaces (no direct entity sharing across modules)

---

## Implementation Roadmap

### Phase 1 — Foundation (First Implementation)

1. Backend project setup (Quarkus, Maven, PostgreSQL, Flyway, JWT, Hibernate Panache)
2. Database migrations: users, profiles, authors, publishers, books
3. Identity module: User registration, login, JWT issuance, role management, profile CRUD
4. Catalog module: Author CRUD, Publisher CRUD, Book CRUD
5. Event infrastructure: in-process event bus with interface for future broker swap
6. Unit tests for business rules (duplicate email, role checks, ISBN uniqueness)
7. Integration tests for all Phase 1 endpoints
8. Frontend project setup (React, TypeScript, Vite, Axios, React Router)
9. Frontend screens: Login, Register, Book list, Book detail, Admin book/author/publisher forms

### Phase 2 — Reading Module

1. Database migration: readings table
2. Reading module: create, update, delete, list readings
3. Rating and review validation
4. Reading events published to Event_Bus
5. Frontend screens: My Readings, Reading detail, Review/rating form

### Phase 3 — Recommendation Service (Future)

1. Stand up Recommendation_Service as a separate deployable (microservice extraction)
2. Connect to message broker (e.g., Apache Kafka or Amazon SQS)
3. Implement LLM integration for book suggestions based on user ratings and reviews
4. Expose `GET /recommendations` endpoint
5. Frontend screen: Recommendations page

### Phase 4 — Frontend Enhancements (F1–F4)

1. **F1 — Internationalization**: Install `react-i18next` + `i18next`; create `src/i18n/index.ts` and locale JSON files; wrap app with `I18nextProvider`; add `LanguageSwitcher` to Navbar; translate all static UI text.
2. **F2 — Language-aware Recommendations**: Implement `getLanguageInstruction` utility; integrate locale into AI API prompt construction; ensure recommendation text uses i18n keys for rule-based content.
3. **F4 — Login Screen Background**: Redesign `LoginPage` with split-screen layout; add library background image with gradient overlay; add translated inspirational quote; implement mobile responsive fallback.
4. **F3 — Automatic Cover Search**: Implement `bookCoverService` with Open Library API integration; build `BookCoverFetcher` component; integrate into book registration and edit forms; display cover thumbnails in book cards and list views.
