package com.library.reading.api;

import com.library.identity.application.UserService;
import com.library.infrastructure.PostgresTestResource;
import com.library.infrastructure.TestEventObserver;
import com.library.reading.application.event.RatingUpdatedPayload;
import com.library.reading.application.event.ReadingCreatedPayload;
import com.library.reading.application.event.ReviewSubmittedPayload;
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
 * Integration tests for the Reading module REST endpoints.
 *
 * <p>Covers:
 * <ul>
 *   <li>POST /api/v1/readings — 201, 409</li>
 *   <li>GET  /api/v1/readings — 200 with pagination and status filter</li>
 *   <li>GET  /api/v1/readings/{id} — 200, 403 cross-user</li>
 *   <li>PUT  /api/v1/readings/{id} — 200, 400 (rating), 400 (FINISHED without startedAt), 403 cross-user</li>
 *   <li>DELETE /api/v1/readings/{id} — 204, 403 cross-user</li>
 * </ul>
 *
 * <p>Domain events are verified via {@link TestEventObserver}.
 *
 * <p>Requirements: 8.1–8.14, 9.6–9.8, 11.5, NFR 4.2
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class ReadingResourceIT {

    @Inject
    TestEventObserver eventObserver;

    @Inject
    UserService userService;

    /** JWT for a regular USER, refreshed before each test. */
    private String userToken;
    private String userId;

    /** JWT for a second USER (cross-user tests). */
    private String otherUserToken;
    private String otherUserId;

    /** JWT for an ADMIN user. */
    private String adminToken;

    /** A book ID created once per test for reading operations. */
    private String bookId;

    @BeforeEach
    void setUp() {
        eventObserver.clear();
        adminToken = bootstrapAdminToken();

        var user = bootstrapUser();
        userToken = user[0];
        userId    = user[1];

        var other = bootstrapUser();
        otherUserToken = other[0];
        otherUserId    = other[1];

        bookId = createBook();
    }

    // ── bootstrap helpers ─────────────────────────────────────────────────────

    private String bootstrapAdminToken() {
        String email    = "admin-" + UUID.randomUUID() + "@test.com";
        String password = "adminPass99";

        given().contentType(ContentType.JSON)
                .body(Map.of("name", "Admin", "email", email, "password", password))
                .post("/api/v1/auth/register")
                .then().statusCode(201);

        String tempToken = given().contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", password))
                .post("/api/v1/auth/login")
                .then().statusCode(200)
                .extract().path("token");

        String id = given().header("Authorization", "Bearer " + tempToken)
                .get("/api/v1/users/me")
                .then().statusCode(200)
                .extract().path("id");

        userService.updateRole(UUID.fromString(id), com.library.identity.domain.Role.ADMIN);

        return given().contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", password))
                .post("/api/v1/auth/login")
                .then().statusCode(200)
                .extract().path("token");
    }

    /** Returns [token, userId]. */
    private String[] bootstrapUser() {
        String email    = "user-" + UUID.randomUUID() + "@test.com";
        String password = "userPass99";

        given().contentType(ContentType.JSON)
                .body(Map.of("name", "User", "email", email, "password", password))
                .post("/api/v1/auth/register")
                .then().statusCode(201);

        String token = given().contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", password))
                .post("/api/v1/auth/login")
                .then().statusCode(200)
                .extract().path("token");

        String id = given().header("Authorization", "Bearer " + token)
                .get("/api/v1/users/me")
                .then().statusCode(200)
                .extract().path("id");

        return new String[]{token, id};
    }

    /** Creates an author, publisher, and book as ADMIN; returns the book ID. */
    private String createBook() {
        String authorId = given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + adminToken)
                .body(Map.of("name", "Author-" + UUID.randomUUID()))
                .post("/api/v1/authors")
                .then().statusCode(201)
                .extract().path("id");

        String publisherId = given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + adminToken)
                .body(Map.of("name", "Publisher-" + UUID.randomUUID()))
                .post("/api/v1/publishers")
                .then().statusCode(201)
                .extract().path("id");

        String isbn = String.valueOf(System.nanoTime()).substring(0, 13);

        return given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + adminToken)
                .body(Map.of("isbn", isbn, "title", "Book-" + UUID.randomUUID(),
                        "authorId", authorId, "publisherId", publisherId))
                .post("/api/v1/books")
                .then().statusCode(201)
                .extract().path("id");
    }

    /** Creates a reading for the current user and returns its ID. */
    private String createReading(String token, String bId) {
        return given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(Map.of("bookId", bId))
                .post("/api/v1/readings")
                .then().statusCode(201)
                .extract().path("id");
    }

    // =========================================================================
    // POST /api/v1/readings
    // =========================================================================

    /**
     * Requirement 8.1: Creating a reading sets status to WANT_TO_READ and returns 201.
     */
    @Test
    void createReading_withValidBookId_returns201WithWantToRead() {
        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + userToken)
                .body(Map.of("bookId", bookId))
                .post("/api/v1/readings")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("status", equalTo("WANT_TO_READ"))
                .body("book.id", equalTo(bookId))
                .body("rating", nullValue())
                .body("review", nullValue())
                .body("createdAt", notNullValue());
    }

    /**
     * Requirement 8.2: Duplicate reading (same user + book) returns 409.
     */
    @Test
    void createReading_duplicate_returns409() {
        createReading(userToken, bookId);

        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + userToken)
                .body(Map.of("bookId", bookId))
                .post("/api/v1/readings")
                .then()
                .statusCode(409)
                .body("status", equalTo(409));
    }

    /**
     * Requirement 2.4: Creating a reading without a JWT returns 401.
     */
    @Test
    void createReading_withoutToken_returns401() {
        given().contentType(ContentType.JSON)
                .body(Map.of("bookId", bookId))
                .post("/api/v1/readings")
                .then()
                .statusCode(401);
    }

    /**
     * Requirement 8.11 + 9.6: reading.created event is published with correct payload.
     */
    @Test
    void createReading_publishesReadingCreatedEvent() {
        eventObserver.clear();

        String readingId = createReading(userToken, bookId);

        var events = eventObserver.getEventsOfType("reading.created");
        assertEquals(1, events.size(), "Expected exactly one reading.created event");

        ReadingCreatedPayload payload = (ReadingCreatedPayload) events.get(0).payload();
        assertEquals(UUID.fromString(readingId), payload.readingId());
        assertEquals(UUID.fromString(userId), payload.userId());
        assertEquals(UUID.fromString(bookId), payload.bookId());
        assertEquals("WANT_TO_READ", payload.status());
    }

    // =========================================================================
    // GET /api/v1/readings
    // =========================================================================

    /**
     * Requirement 8.6 + 11.1 + 11.5: List readings returns paginated response.
     */
    @Test
    void listReadings_returnsPagedResponse() {
        createReading(userToken, bookId);

        given().header("Authorization", "Bearer " + userToken)
                .get("/api/v1/readings?page=0&size=20")
                .then()
                .statusCode(200)
                .body("content", notNullValue())
                .body("totalElements", greaterThanOrEqualTo(1))
                .body("totalPages", greaterThanOrEqualTo(1))
                .body("page", equalTo(0))
                .body("size", equalTo(20));
    }

    /**
     * Requirement 11.1: Default pagination uses page=0, size=20.
     */
    @Test
    void listReadings_withNoParams_usesDefaults() {
        given().header("Authorization", "Bearer " + userToken)
                .get("/api/v1/readings")
                .then()
                .statusCode(200)
                .body("page", equalTo(0))
                .body("size", equalTo(20));
    }

    /**
     * Requirement 11.5: Status filter returns only readings with the given status.
     */
    @Test
    void listReadings_withStatusFilter_returnsOnlyMatchingReadings() {
        createReading(userToken, bookId);

        // Filter by WANT_TO_READ — should include the newly created reading
        given().header("Authorization", "Bearer " + userToken)
                .get("/api/v1/readings?status=WANT_TO_READ")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].status", equalTo("WANT_TO_READ"));

        // Filter by FINISHED — should return empty for this user
        given().header("Authorization", "Bearer " + userToken)
                .get("/api/v1/readings?status=FINISHED")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(0));
    }

    /**
     * Requirement 8.6: List only returns the requesting user's own readings.
     */
    @Test
    void listReadings_returnsOnlyOwnReadings() {
        // Create a second book for the other user
        String otherBookId = createBook();
        createReading(userToken, bookId);
        createReading(otherUserToken, otherBookId);

        // User should only see their own reading
        given().header("Authorization", "Bearer " + userToken)
                .get("/api/v1/readings")
                .then()
                .statusCode(200)
                .body("content.findAll { it.book.id == '" + otherBookId + "' }.size()", equalTo(0));
    }

    // =========================================================================
    // GET /api/v1/readings/{id}
    // =========================================================================

    /**
     * Requirement 8.7: Owner can retrieve their reading by ID.
     */
    @Test
    void getReadingById_ownReading_returns200() {
        String readingId = createReading(userToken, bookId);

        given().header("Authorization", "Bearer " + userToken)
                .get("/api/v1/readings/" + readingId)
                .then()
                .statusCode(200)
                .body("id", equalTo(readingId))
                .body("status", equalTo("WANT_TO_READ"))
                .body("book.id", equalTo(bookId));
    }

    /**
     * Requirement 8.8: Accessing another user's reading by ID returns 403.
     */
    @Test
    void getReadingById_otherUsersReading_returns403() {
        String readingId = createReading(userToken, bookId);

        given().header("Authorization", "Bearer " + otherUserToken)
                .get("/api/v1/readings/" + readingId)
                .then()
                .statusCode(403)
                .body("status", equalTo(403));
    }

    // =========================================================================
    // PUT /api/v1/readings/{id}
    // =========================================================================

    /**
     * Requirement 8.3: Owner can update status, rating, review, startedAt, finishedAt.
     */
    @Test
    void updateReading_withValidData_returns200() {
        String readingId = createReading(userToken, bookId);

        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + userToken)
                .body(Map.of(
                        "status", "READING",
                        "rating", 7.5,
                        "review", "Great so far!",
                        "startedAt", "2024-01-15T10:00:00Z"))
                .put("/api/v1/readings/" + readingId)
                .then()
                .statusCode(200)
                .body("status", equalTo("READING"))
                .body("rating", equalTo(7.5f))
                .body("review", equalTo("Great so far!"))
                .body("startedAt", notNullValue());
    }

    /**
     * Requirement 8.9: Rating outside 0.0–10.0 returns 400.
     */
    @Test
    void updateReading_withRatingAbove10_returns400() {
        String readingId = createReading(userToken, bookId);

        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + userToken)
                .body(Map.of("rating", 10.1))
                .put("/api/v1/readings/" + readingId)
                .then()
                .statusCode(400)
                .body("status", equalTo(400));
    }

    /**
     * Requirement 8.9: Negative rating returns 400.
     */
    @Test
    void updateReading_withNegativeRating_returns400() {
        String readingId = createReading(userToken, bookId);

        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + userToken)
                .body(Map.of("rating", -0.1))
                .put("/api/v1/readings/" + readingId)
                .then()
                .statusCode(400)
                .body("status", equalTo(400));
    }

    /**
     * Requirement 8.10: Setting status to FINISHED without startedAt returns 400.
     */
    @Test
    void updateReading_finishedWithoutStartedAt_returns400() {
        String readingId = createReading(userToken, bookId);

        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + userToken)
                .body(Map.of("status", "FINISHED"))
                .put("/api/v1/readings/" + readingId)
                .then()
                .statusCode(400)
                .body("status", equalTo(400));
    }

    /**
     * Requirement 8.10: Setting status to FINISHED with startedAt succeeds.
     */
    @Test
    void updateReading_finishedWithStartedAt_returns200() {
        String readingId = createReading(userToken, bookId);

        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + userToken)
                .body(Map.of(
                        "status", "FINISHED",
                        "startedAt", "2024-01-01T00:00:00Z",
                        "finishedAt", "2024-02-01T00:00:00Z"))
                .put("/api/v1/readings/" + readingId)
                .then()
                .statusCode(200)
                .body("status", equalTo("FINISHED"))
                .body("startedAt", notNullValue())
                .body("finishedAt", notNullValue());
    }

    /**
     * Requirement 8.5: Updating another user's reading returns 403.
     */
    @Test
    void updateReading_otherUsersReading_returns403() {
        String readingId = createReading(userToken, bookId);

        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + otherUserToken)
                .body(Map.of("status", "READING"))
                .put("/api/v1/readings/" + readingId)
                .then()
                .statusCode(403)
                .body("status", equalTo(403));
    }

    /**
     * Requirement 8.4: ADMIN can update any user's reading.
     */
    @Test
    void updateReading_byAdmin_returns200() {
        String readingId = createReading(userToken, bookId);

        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + adminToken)
                .body(Map.of("status", "READING"))
                .put("/api/v1/readings/" + readingId)
                .then()
                .statusCode(200)
                .body("status", equalTo("READING"));
    }

    /**
     * Requirement 8.12 + 9.7: rating.updated event is published when rating changes.
     */
    @Test
    void updateReading_withNewRating_publishesRatingUpdatedEvent() {
        String readingId = createReading(userToken, bookId);
        eventObserver.clear();

        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + userToken)
                .body(Map.of("rating", 8.0))
                .put("/api/v1/readings/" + readingId)
                .then().statusCode(200);

        var events = eventObserver.getEventsOfType("rating.updated");
        assertEquals(1, events.size(), "Expected exactly one rating.updated event");

        RatingUpdatedPayload payload = (RatingUpdatedPayload) events.get(0).payload();
        assertEquals(UUID.fromString(readingId), payload.readingId());
        assertEquals(UUID.fromString(userId), payload.userId());
        assertEquals(UUID.fromString(bookId), payload.bookId());
        assertEquals(0, payload.rating().compareTo(new java.math.BigDecimal("8.0")));
    }

    /**
     * Requirement 8.13 + 9.8: review.submitted event is published when review changes.
     */
    @Test
    void updateReading_withNewReview_publishesReviewSubmittedEvent() {
        String readingId = createReading(userToken, bookId);
        eventObserver.clear();

        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + userToken)
                .body(Map.of("review", "An excellent read!"))
                .put("/api/v1/readings/" + readingId)
                .then().statusCode(200);

        var events = eventObserver.getEventsOfType("review.submitted");
        assertEquals(1, events.size(), "Expected exactly one review.submitted event");

        ReviewSubmittedPayload payload = (ReviewSubmittedPayload) events.get(0).payload();
        assertEquals(UUID.fromString(readingId), payload.readingId());
        assertEquals(UUID.fromString(userId), payload.userId());
        assertEquals(UUID.fromString(bookId), payload.bookId());
        assertEquals("An excellent read!", payload.review());
    }

    /**
     * Requirement 8.12: rating.updated event is NOT published when rating is unchanged.
     */
    @Test
    void updateReading_withSameRating_doesNotPublishRatingUpdatedEvent() {
        String readingId = createReading(userToken, bookId);

        // Set initial rating
        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + userToken)
                .body(Map.of("rating", 5.0))
                .put("/api/v1/readings/" + readingId)
                .then().statusCode(200);

        eventObserver.clear();

        // Update with same rating — no event expected
        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + userToken)
                .body(Map.of("rating", 5.0))
                .put("/api/v1/readings/" + readingId)
                .then().statusCode(200);

        var events = eventObserver.getEventsOfType("rating.updated");
        assertEquals(0, events.size(), "Expected no rating.updated event when rating is unchanged");
    }

    // =========================================================================
    // DELETE /api/v1/readings/{id}
    // =========================================================================

    /**
     * Requirement 8.14: Owner can delete their own reading — returns 204.
     */
    @Test
    void deleteReading_ownReading_returns204() {
        String readingId = createReading(userToken, bookId);

        given().header("Authorization", "Bearer " + userToken)
                .delete("/api/v1/readings/" + readingId)
                .then()
                .statusCode(204);

        // Verify it's gone
        given().header("Authorization", "Bearer " + userToken)
                .get("/api/v1/readings/" + readingId)
                .then()
                .statusCode(404);
    }

    /**
     * Requirement 8.5: Deleting another user's reading returns 403.
     */
    @Test
    void deleteReading_otherUsersReading_returns403() {
        String readingId = createReading(userToken, bookId);

        given().header("Authorization", "Bearer " + otherUserToken)
                .delete("/api/v1/readings/" + readingId)
                .then()
                .statusCode(403)
                .body("status", equalTo(403));
    }

    /**
     * Requirement 8.4: ADMIN can delete any user's reading.
     */
    @Test
    void deleteReading_byAdmin_returns204() {
        String readingId = createReading(userToken, bookId);

        given().header("Authorization", "Bearer " + adminToken)
                .delete("/api/v1/readings/" + readingId)
                .then()
                .statusCode(204);
    }

    /**
     * Requirement 12.1: Error responses have consistent JSON structure.
     */
    @Test
    void errorResponse_hasConsistentStructure() {
        // Trigger a 409 via duplicate reading
        createReading(userToken, bookId);

        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + userToken)
                .body(Map.of("bookId", bookId))
                .post("/api/v1/readings")
                .then()
                .statusCode(409)
                .body("status", notNullValue())
                .body("message", notNullValue())
                .body("timestamp", notNullValue());
    }
}
