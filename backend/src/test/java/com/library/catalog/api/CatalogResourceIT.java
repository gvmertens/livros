package com.library.catalog.api;

import com.library.catalog.application.event.BookCreatedPayload;
import com.library.catalog.application.event.BookDeletedPayload;
import com.library.catalog.application.event.BookUpdatedPayload;
import com.library.identity.application.UserService;
import com.library.infrastructure.PostgresTestResource;
import com.library.infrastructure.TestEventObserver;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the Catalog module REST endpoints.
 *
 * Uses real JWT tokens throughout (no @TestSecurity) to avoid Quarkus app
 * restarts that break Panache bytecode enhancement.
 *
 * An ADMIN token is bootstrapped in @BeforeEach by:
 *   1. Registering a fresh admin-candidate user via the API
 *   2. Promoting them to ADMIN via injected UserService (bypasses the
 *      chicken-and-egg problem of needing ADMIN to call PUT /users/{id}/role)
 *   3. Re-logging in to get a JWT with the ADMIN role claim
 *
 * Requirements: 5.1-5.7, 6.1-6.7, 7.1-7.12, 11.1-11.4, NFR 4.2
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class CatalogResourceIT {

    @Inject
    TestEventObserver eventObserver;

    @Inject
    UserService userService;

    /** JWT for an ADMIN user, refreshed before each test. */
    private String adminToken;

    /** JWT for a regular USER, refreshed before each test. */
    private String userToken;

    @BeforeEach
    void setUp() {
        eventObserver.clear();
        adminToken = bootstrapAdminToken();
        userToken  = bootstrapUserToken();
    }

    // ── bootstrap helpers ─────────────────────────────────────────────────────

    private String bootstrapAdminToken() {
        String email    = "admin-" + UUID.randomUUID() + "@test.com";
        String password = "adminPass99";

        // 1. Register
        given().contentType(ContentType.JSON)
                .body(Map.of("name", "Admin User", "email", email, "password", password))
                .post("/api/v1/auth/register")
                .then().statusCode(201);

        // 2. Get the new user's ID
        String tempToken = given().contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", password))
                .post("/api/v1/auth/login")
                .then().statusCode(200)
                .extract().path("token");

        String userId = given().header("Authorization", "Bearer " + tempToken)
                .get("/api/v1/users/me")
                .then().statusCode(200)
                .extract().path("id");

        // 3. Promote to ADMIN via injected service (no HTTP round-trip needed)
        userService.updateRole(UUID.fromString(userId), com.library.identity.domain.Role.ADMIN);

        // 4. Re-login to get a JWT that carries the ADMIN role claim
        return given().contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", password))
                .post("/api/v1/auth/login")
                .then().statusCode(200)
                .extract().path("token");
    }

    private String bootstrapUserToken() {
        String email    = "user-" + UUID.randomUUID() + "@test.com";
        String password = "userPass99";

        given().contentType(ContentType.JSON)
                .body(Map.of("name", "Regular User", "email", email, "password", password))
                .post("/api/v1/auth/register")
                .then().statusCode(201);

        return given().contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", password))
                .post("/api/v1/auth/login")
                .then().statusCode(200)
                .extract().path("token");
    }

    /** Creates an author as ADMIN and returns its UUID string. */
    private String createAuthor(String name) {
        return given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + adminToken)
                .body(Map.of("name", name))
                .post("/api/v1/authors")
                .then().statusCode(201)
                .extract().path("id");
    }

    /** Creates a publisher as ADMIN and returns its UUID string. */
    private String createPublisher(String name) {
        return given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + adminToken)
                .body(Map.of("name", name))
                .post("/api/v1/publishers")
                .then().statusCode(201)
                .extract().path("id");
    }

    /** Creates a book as ADMIN and returns its UUID string. */
    private String createBook(String isbn, String title, String authorId, String publisherId) {
        return given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + adminToken)
                .body(Map.of("isbn", isbn, "title", title,
                        "authorId", authorId, "publisherId", publisherId))
                .post("/api/v1/books")
                .then().statusCode(201)
                .extract().path("id");
    }

    /** Generates a unique 13-char ISBN-like string to avoid conflicts between tests. */
    private String uniqueIsbn() {
        String nano = String.valueOf(System.nanoTime());
        return nano.length() >= 13 ? nano.substring(nano.length() - 13) : nano;
    }

    // =========================================================================
    // AUTHOR TESTS
    // =========================================================================

    /** Requirement 5.1: ADMIN creates an author — returns 201. */
    @Test
    void createAuthor_withValidName_returns201() {
        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + adminToken)
                .body(Map.of("name", "George Orwell"))
                .post("/api/v1/authors")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("George Orwell"))
                .body("createdAt", notNullValue());
    }

    /** Requirement 5.7: Blank name returns 400. */
    @Test
    void createAuthor_withBlankName_returns400() {
        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + adminToken)
                .body(Map.of("name", ""))
                .post("/api/v1/authors")
                .then()
                .statusCode(400);
    }

    /** Requirement 3.2: USER cannot create an author — returns 403. */
    @Test
    void createAuthor_withUserRole_returns403() {
        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + userToken)
                .body(Map.of("name", "Forbidden Author"))
                .post("/api/v1/authors")
                .then()
                .statusCode(403);
    }

    /** Requirement 5.5 + 11.1 + 11.3: List authors returns paginated response. */
    @Test
    void listAuthors_returnsPagedResponse() {
        given().header("Authorization", "Bearer " + userToken)
                .get("/api/v1/authors?page=0&size=10")
                .then()
                .statusCode(200)
                .body("content", notNullValue())
                .body("totalElements", greaterThanOrEqualTo(0))
                .body("totalPages", greaterThanOrEqualTo(0))
                .body("page", equalTo(0))
                .body("size", equalTo(10));
    }

    /** Requirement 11.1: Default pagination uses page=0, size=20. */
    @Test
    void listAuthors_withNoParams_usesDefaults() {
        given().header("Authorization", "Bearer " + userToken)
                .get("/api/v1/authors")
                .then()
                .statusCode(200)
                .body("page", equalTo(0))
                .body("size", equalTo(20));
    }

    /** Requirement 5.6: Get author by ID returns 200. */
    @Test
    void getAuthorById_existingId_returns200() {
        String id = createAuthor("Jane Austen");

        given().header("Authorization", "Bearer " + userToken)
                .get("/api/v1/authors/" + id)
                .then()
                .statusCode(200)
                .body("id", equalTo(id))
                .body("name", equalTo("Jane Austen"));
    }

    /** Requirement 5.6: Non-existent author ID returns 404. */
    @Test
    void getAuthorById_nonExistentId_returns404() {
        given().header("Authorization", "Bearer " + userToken)
                .get("/api/v1/authors/" + UUID.randomUUID())
                .then()
                .statusCode(404)
                .body("status", equalTo(404));
    }

    /** Requirement 5.2: ADMIN can update an author's name. */
    @Test
    void updateAuthor_withValidName_returns200() {
        String id = createAuthor("Old Name");

        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + adminToken)
                .body(Map.of("name", "New Name"))
                .put("/api/v1/authors/" + id)
                .then()
                .statusCode(200)
                .body("name", equalTo("New Name"));
    }

    /** Requirement 5.7: Update with blank name returns 400. */
    @Test
    void updateAuthor_withBlankName_returns400() {
        String id = createAuthor("Valid Author");

        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + adminToken)
                .body(Map.of("name", ""))
                .put("/api/v1/authors/" + id)
                .then()
                .statusCode(400);
    }

    /** Requirement 5.3: ADMIN deletes author with no books — returns 204. */
    @Test
    void deleteAuthor_withNoBooks_returns204() {
        String id = createAuthor("Deletable Author");

        given().header("Authorization", "Bearer " + adminToken)
                .delete("/api/v1/authors/" + id)
                .then()
                .statusCode(204);

        given().header("Authorization", "Bearer " + userToken)
                .get("/api/v1/authors/" + id)
                .then()
                .statusCode(404);
    }

    /** Requirement 5.4: Deleting author with associated books returns 409. */
    @Test
    void deleteAuthor_withAssociatedBooks_returns409() {
        String authorId    = createAuthor("Author With Books");
        String publisherId = createPublisher("Publisher For Author Test");
        createBook(uniqueIsbn(), "Some Book", authorId, publisherId);

        given().header("Authorization", "Bearer " + adminToken)
                .delete("/api/v1/authors/" + authorId)
                .then()
                .statusCode(409)
                .body("status", equalTo(409));
    }

    // =========================================================================
    // PUBLISHER TESTS
    // =========================================================================

    /** Requirement 6.1: ADMIN creates a publisher — returns 201. */
    @Test
    void createPublisher_withValidName_returns201() {
        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + adminToken)
                .body(Map.of("name", "Penguin Books"))
                .post("/api/v1/publishers")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("Penguin Books"));
    }

    /** Requirement 6.7: Blank publisher name returns 400. */
    @Test
    void createPublisher_withBlankName_returns400() {
        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + adminToken)
                .body(Map.of("name", ""))
                .post("/api/v1/publishers")
                .then()
                .statusCode(400);
    }

    /** Requirement 3.3: USER cannot create a publisher — returns 403. */
    @Test
    void createPublisher_withUserRole_returns403() {
        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + userToken)
                .body(Map.of("name", "Forbidden Publisher"))
                .post("/api/v1/publishers")
                .then()
                .statusCode(403);
    }

    /** Requirement 6.5 + 11.1: List publishers returns paginated response. */
    @Test
    void listPublishers_returnsPagedResponse() {
        given().header("Authorization", "Bearer " + userToken)
                .get("/api/v1/publishers?page=0&size=20")
                .then()
                .statusCode(200)
                .body("content", notNullValue())
                .body("page", equalTo(0))
                .body("size", equalTo(20));
    }

    /** Requirement 11.1: Default pagination for publishers uses page=0, size=20. */
    @Test
    void listPublishers_withNoParams_usesDefaults() {
        given().header("Authorization", "Bearer " + userToken)
                .get("/api/v1/publishers")
                .then()
                .statusCode(200)
                .body("page", equalTo(0))
                .body("size", equalTo(20));
    }

    /** Requirement 6.6: Non-existent publisher ID returns 404. */
    @Test
    void getPublisherById_nonExistentId_returns404() {
        given().header("Authorization", "Bearer " + userToken)
                .get("/api/v1/publishers/" + UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    /** Requirement 6.2: ADMIN can update a publisher's name. */
    @Test
    void updatePublisher_withValidName_returns200() {
        String id = createPublisher("Old Publisher");

        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + adminToken)
                .body(Map.of("name", "Updated Publisher"))
                .put("/api/v1/publishers/" + id)
                .then()
                .statusCode(200)
                .body("name", equalTo("Updated Publisher"));
    }

    /** Requirement 6.3: ADMIN deletes publisher with no books — returns 204. */
    @Test
    void deletePublisher_withNoBooks_returns204() {
        String id = createPublisher("Deletable Publisher");

        given().header("Authorization", "Bearer " + adminToken)
                .delete("/api/v1/publishers/" + id)
                .then()
                .statusCode(204);
    }

    /** Requirement 6.4: Deleting publisher with associated books returns 409. */
    @Test
    void deletePublisher_withAssociatedBooks_returns409() {
        String authorId    = createAuthor("Author For Publisher Test");
        String publisherId = createPublisher("Publisher With Books");
        createBook(uniqueIsbn(), "Publisher Book", authorId, publisherId);

        given().header("Authorization", "Bearer " + adminToken)
                .delete("/api/v1/publishers/" + publisherId)
                .then()
                .statusCode(409)
                .body("status", equalTo(409));
    }

    // =========================================================================
    // BOOK TESTS
    // =========================================================================

    /** Requirement 7.1: ADMIN creates a book — returns 201 with embedded author/publisher. */
    @Test
    void createBook_withValidData_returns201() {
        String authorId    = createAuthor("Tolkien");
        String publisherId = createPublisher("Allen & Unwin");
        String isbn        = uniqueIsbn();

        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + adminToken)
                .body(Map.of("isbn", isbn, "title", "The Hobbit",
                        "authorId", authorId, "publisherId", publisherId))
                .post("/api/v1/books")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("isbn", equalTo(isbn))
                .body("title", equalTo("The Hobbit"))
                .body("author.id", equalTo(authorId))
                .body("author.name", equalTo("Tolkien"))
                .body("publisher.id", equalTo(publisherId))
                .body("publisher.name", equalTo("Allen & Unwin"))
                .body("createdAt", notNullValue());
    }

    /** Requirement 7.2: Duplicate ISBN returns 409. */
    @Test
    void createBook_withDuplicateIsbn_returns409() {
        String authorId    = createAuthor("Author Dup ISBN");
        String publisherId = createPublisher("Publisher Dup ISBN");
        String isbn        = uniqueIsbn();

        createBook(isbn, "First Book", authorId, publisherId);

        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + adminToken)
                .body(Map.of("isbn", isbn, "title", "Second Book",
                        "authorId", authorId, "publisherId", publisherId))
                .post("/api/v1/books")
                .then()
                .statusCode(409)
                .body("status", equalTo(409));
    }

    /** Requirement 7.7: Non-existent authorId returns 422. */
    @Test
    void createBook_withNonExistentAuthorId_returns422() {
        String publisherId = createPublisher("Publisher 422 Author");

        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + adminToken)
                .body(Map.of("isbn", uniqueIsbn(), "title", "Orphan Book",
                        "authorId", UUID.randomUUID().toString(),
                        "publisherId", publisherId))
                .post("/api/v1/books")
                .then()
                .statusCode(422)
                .body("status", equalTo(422));
    }

    /** Requirement 7.8: Non-existent publisherId returns 422. */
    @Test
    void createBook_withNonExistentPublisherId_returns422() {
        String authorId = createAuthor("Author 422 Publisher");

        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + adminToken)
                .body(Map.of("isbn", uniqueIsbn(), "title", "Orphan Book",
                        "authorId", authorId,
                        "publisherId", UUID.randomUUID().toString()))
                .post("/api/v1/books")
                .then()
                .statusCode(422)
                .body("status", equalTo(422));
    }

    /** Requirement 3.1: USER cannot create a book — returns 403. */
    @Test
    void createBook_withUserRole_returns403() {
        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + userToken)
                .body(Map.of("isbn", uniqueIsbn(), "title", "Forbidden Book",
                        "authorId", UUID.randomUUID().toString(),
                        "publisherId", UUID.randomUUID().toString()))
                .post("/api/v1/books")
                .then()
                .statusCode(403);
    }

    /** Requirement 7.5 + 11.1 + 11.3: List books returns paginated response with metadata. */
    @Test
    void listBooks_returnsPagedResponse() {
        given().header("Authorization", "Bearer " + userToken)
                .get("/api/v1/books?page=0&size=20")
                .then()
                .statusCode(200)
                .body("content", notNullValue())
                .body("totalElements", greaterThanOrEqualTo(0))
                .body("totalPages", greaterThanOrEqualTo(0))
                .body("page", equalTo(0))
                .body("size", equalTo(20));
    }

    /** Requirement 11.1: Default pagination uses page=0, size=20. */
    @Test
    void listBooks_withNoParams_usesDefaults() {
        given().header("Authorization", "Bearer " + userToken)
                .get("/api/v1/books")
                .then()
                .statusCode(200)
                .body("page", equalTo(0))
                .body("size", equalTo(20));
    }

    /** Requirement 11.2: Page size > 100 returns 400. */
    @Test
    void listBooks_withOversizedPage_returns400() {
        given().header("Authorization", "Bearer " + userToken)
                .get("/api/v1/books?page=0&size=101")
                .then()
                .statusCode(400);
    }

    /** Requirement 7.12 + 11.4: Search by title returns only matching books (case-insensitive). */
    @Test
    void listBooks_withSearchQuery_returnsMatchingBooks() {
        String authorId    = createAuthor("Search Author");
        String publisherId = createPublisher("Search Publisher");
        String uniqueTitle = "UniqueTitle" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        createBook(uniqueIsbn(), uniqueTitle, authorId, publisherId);

        given().header("Authorization", "Bearer " + userToken)
                .get("/api/v1/books?search=" + uniqueTitle.toLowerCase())
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].title", equalTo(uniqueTitle));
    }

    /** Requirement 7.12: Search by author name returns matching books. */
    @Test
    void listBooks_searchByAuthorName_returnsMatchingBooks() {
        String uniqueAuthor = "UniqueAuthor" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String authorId     = createAuthor(uniqueAuthor);
        String publisherId  = createPublisher("Publisher For Author Search");
        createBook(uniqueIsbn(), "Book By Unique Author", authorId, publisherId);

        given().header("Authorization", "Bearer " + userToken)
                .get("/api/v1/books?search=" + uniqueAuthor.toLowerCase())
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));
    }

    /** Requirement 7.6: Get book by ID returns 200 with full details. */
    @Test
    void getBookById_existingId_returns200() {
        String authorId    = createAuthor("Author GetById");
        String publisherId = createPublisher("Publisher GetById");
        String isbn        = uniqueIsbn();
        String bookId      = createBook(isbn, "Get By ID Book", authorId, publisherId);

        given().header("Authorization", "Bearer " + userToken)
                .get("/api/v1/books/" + bookId)
                .then()
                .statusCode(200)
                .body("id", equalTo(bookId))
                .body("isbn", equalTo(isbn))
                .body("title", equalTo("Get By ID Book"))
                .body("author.id", equalTo(authorId))
                .body("publisher.id", equalTo(publisherId));
    }

    /** Requirement 7.6: Non-existent book ID returns 404. */
    @Test
    void getBookById_nonExistentId_returns404() {
        given().header("Authorization", "Bearer " + userToken)
                .get("/api/v1/books/" + UUID.randomUUID())
                .then()
                .statusCode(404)
                .body("status", equalTo(404));
    }

    /** Requirement 7.3: ADMIN can update a book's fields. */
    @Test
    void updateBook_withValidData_returns200() {
        String authorId    = createAuthor("Author Update");
        String publisherId = createPublisher("Publisher Update");
        String bookId      = createBook(uniqueIsbn(), "Original Title", authorId, publisherId);
        String newAuthorId = createAuthor("New Author");
        String newIsbn     = uniqueIsbn();

        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + adminToken)
                .body(Map.of("isbn", newIsbn, "title", "Updated Title",
                        "authorId", newAuthorId, "publisherId", publisherId))
                .put("/api/v1/books/" + bookId)
                .then()
                .statusCode(200)
                .body("title", equalTo("Updated Title"))
                .body("isbn", equalTo(newIsbn))
                .body("author.id", equalTo(newAuthorId));
    }

    /** Requirement 3.1: USER cannot update a book — returns 403. */
    @Test
    void updateBook_withUserRole_returns403() {
        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + userToken)
                .body(Map.of("isbn", uniqueIsbn(), "title", "Forbidden Update",
                        "authorId", UUID.randomUUID().toString(),
                        "publisherId", UUID.randomUUID().toString()))
                .put("/api/v1/books/" + UUID.randomUUID())
                .then()
                .statusCode(403);
    }

    /** Requirement 7.4: ADMIN deletes a book — returns 204. */
    @Test
    void deleteBook_existingBook_returns204() {
        String authorId    = createAuthor("Author Delete");
        String publisherId = createPublisher("Publisher Delete");
        String bookId      = createBook(uniqueIsbn(), "Book To Delete", authorId, publisherId);

        given().header("Authorization", "Bearer " + adminToken)
                .delete("/api/v1/books/" + bookId)
                .then()
                .statusCode(204);

        given().header("Authorization", "Bearer " + userToken)
                .get("/api/v1/books/" + bookId)
                .then()
                .statusCode(404);
    }

    /** Requirement 3.1: USER cannot delete a book — returns 403. */
    @Test
    void deleteBook_withUserRole_returns403() {
        given().header("Authorization", "Bearer " + userToken)
                .delete("/api/v1/books/" + UUID.randomUUID())
                .then()
                .statusCode(403);
    }

    // ── Domain events ─────────────────────────────────────────────────────────

    /** Requirement 7.9 + 9.3: book.created event published with correct payload. */
    @Test
    void createBook_publishesBookCreatedEvent() {
        String authorId    = createAuthor("Event Author Create");
        String publisherId = createPublisher("Event Publisher Create");
        String isbn        = uniqueIsbn();
        eventObserver.clear();

        String bookId = createBook(isbn, "Event Book Create", authorId, publisherId);

        var events = eventObserver.getEventsOfType("book.created");
        assertEquals(1, events.size(), "Expected exactly one book.created event");

        BookCreatedPayload payload = (BookCreatedPayload) events.get(0).payload();
        assertEquals(UUID.fromString(bookId), payload.bookId());
        assertEquals(isbn, payload.isbn());
        assertEquals("Event Book Create", payload.title());
        assertEquals(UUID.fromString(authorId), payload.authorId());
        assertEquals(UUID.fromString(publisherId), payload.publisherId());
    }

    /** Requirement 7.10 + 9.4: book.updated event published with correct payload. */
    @Test
    void updateBook_publishesBookUpdatedEvent() {
        String authorId    = createAuthor("Event Author Update");
        String publisherId = createPublisher("Event Publisher Update");
        String bookId      = createBook(uniqueIsbn(), "Original Event Book", authorId, publisherId);
        String newIsbn     = uniqueIsbn();
        eventObserver.clear();

        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + adminToken)
                .body(Map.of("isbn", newIsbn, "title", "Updated Event Book",
                        "authorId", authorId, "publisherId", publisherId))
                .put("/api/v1/books/" + bookId)
                .then().statusCode(200);

        var events = eventObserver.getEventsOfType("book.updated");
        assertEquals(1, events.size(), "Expected exactly one book.updated event");

        BookUpdatedPayload payload = (BookUpdatedPayload) events.get(0).payload();
        assertEquals(UUID.fromString(bookId), payload.bookId());
        assertEquals("Updated Event Book", payload.title());
    }

    /** Requirement 7.11 + 9.5: book.deleted event published with correct payload. */
    @Test
    void deleteBook_publishesBookDeletedEvent() {
        String authorId    = createAuthor("Event Author Delete");
        String publisherId = createPublisher("Event Publisher Delete");
        String bookId      = createBook(uniqueIsbn(), "Book To Delete Event", authorId, publisherId);
        eventObserver.clear();

        given().header("Authorization", "Bearer " + adminToken)
                .delete("/api/v1/books/" + bookId)
                .then().statusCode(204);

        var events = eventObserver.getEventsOfType("book.deleted");
        assertEquals(1, events.size(), "Expected exactly one book.deleted event");

        BookDeletedPayload payload = (BookDeletedPayload) events.get(0).payload();
        assertEquals(UUID.fromString(bookId), payload.bookId());
    }
}
