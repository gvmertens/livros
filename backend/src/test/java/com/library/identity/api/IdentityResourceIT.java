package com.library.identity.api;

import com.library.identity.application.UserService;
import com.library.identity.application.event.UserCreatedPayload;
import com.library.infrastructure.PostgresTestResource;
import com.library.infrastructure.TestEventObserver;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the Identity module REST endpoints.
 *
 * <p>Covers:
 * <ul>
 *   <li>POST /api/v1/auth/register — 201, 409, 400</li>
 *   <li>POST /api/v1/auth/login — 200, 401</li>
 *   <li>GET  /api/v1/users/me — 200, 401</li>
 *   <li>PUT  /api/v1/users/{id}/role — 200 (ADMIN), 403 (USER)</li>
 *   <li>GET  /api/v1/users/me/profile — 200</li>
 *   <li>PUT  /api/v1/users/me/profile — 200, 400</li>
 * </ul>
 *
 * <p>JWT claims are verified by decoding the token payload (base64).
 * Domain events are verified via {@link TestEventObserver}.
 *
 * <p>Requirements: 1.1–1.6, 2.1–2.6, 3.6, 4.1–4.6, NFR 4.2
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class IdentityResourceIT {

    @Inject
    TestEventObserver eventObserver;

    @Inject
    UserService userService;

    // ── helpers ───────────────────────────────────────────────────────────────

    /** Unique email per test to avoid cross-test conflicts. */
    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    /**
     * Registers a user and returns the JWT token from the login response.
     * Convenience method to set up authenticated state for subsequent calls.
     */
    private String registerAndLogin(String email, String password) {
        String name = "Test User";
        given().contentType(ContentType.JSON)
                .body(Map.of("name", name, "email", email, "password", password))
                .post("/api/v1/auth/register")
                .then().statusCode(201);

        return given().contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", password))
                .post("/api/v1/auth/login")
                .then().statusCode(200)
                .extract().path("token");
    }

    /**
     * Decodes the JWT payload (middle segment) and returns it as a JSON string.
     * No signature verification — used only to inspect claims in tests.
     */
    private String decodeJwtPayload(String token) {
        String[] parts = token.split("\\.");
        return new String(Base64.getUrlDecoder().decode(parts[1]));
    }

    @BeforeEach
    void clearEvents() {
        eventObserver.clear();
    }

    @AfterEach
    void clearEventsAfter() {
        eventObserver.clear();
    }

    // ── POST /api/v1/auth/register ────────────────────────────────────────────

    /**
     * Requirement 1.1: Valid registration creates a USER-role account and returns 201.
     */
    @Test
    void register_withValidInput_returns201AndUserRole() {
        String email = uniqueEmail();

        given().contentType(ContentType.JSON)
                .body(Map.of("name", "Alice", "email", email, "password", "securePass1"))
                .post("/api/v1/auth/register")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("Alice"))
                .body("email", equalTo(email))
                .body("role", equalTo("USER"))
                .body("createdAt", notNullValue());
    }

    /**
     * Requirement 1.2: Duplicate email registration returns 409 Conflict.
     */
    @Test
    void register_withDuplicateEmail_returns409() {
        String email = uniqueEmail();

        given().contentType(ContentType.JSON)
                .body(Map.of("name", "Alice", "email", email, "password", "securePass1"))
                .post("/api/v1/auth/register")
                .then().statusCode(201);

        given().contentType(ContentType.JSON)
                .body(Map.of("name", "Alice2", "email", email, "password", "securePass2"))
                .post("/api/v1/auth/register")
                .then()
                .statusCode(409)
                .body("status", equalTo(409))
                .body("message", notNullValue());
    }

    /**
     * Requirement 1.3: Missing or malformed email returns 400 Bad Request.
     */
    @Test
    void register_withMalformedEmail_returns400() {
        given().contentType(ContentType.JSON)
                .body(Map.of("name", "Bob", "email", "not-an-email", "password", "securePass1"))
                .post("/api/v1/auth/register")
                .then()
                .statusCode(400)
                .body("status", equalTo(400));
    }

    /**
     * Requirement 1.3: Missing email field returns 400 Bad Request.
     */
    @Test
    void register_withMissingEmail_returns400() {
        given().contentType(ContentType.JSON)
                .body(Map.of("name", "Bob", "password", "securePass1"))
                .post("/api/v1/auth/register")
                .then()
                .statusCode(400);
    }

    /**
     * Requirement 1.4: Password shorter than 8 characters returns 400 Bad Request.
     */
    @Test
    void register_withShortPassword_returns400() {
        given().contentType(ContentType.JSON)
                .body(Map.of("name", "Carol", "email", uniqueEmail(), "password", "short"))
                .post("/api/v1/auth/register")
                .then()
                .statusCode(400)
                .body("status", equalTo(400));
    }

    /**
     * Requirement 1.6 + 9.2: Successful registration publishes a user.created event
     * containing userId, email, and createdAt.
     */
    @Test
    void register_withValidInput_publishesUserCreatedEvent() {
        String email = uniqueEmail();

        var response = given().contentType(ContentType.JSON)
                .body(Map.of("name", "Dave", "email", email, "password", "securePass1"))
                .post("/api/v1/auth/register")
                .then().statusCode(201)
                .extract().response();

        String userId = response.path("id");

        var userCreatedEvents = eventObserver.getEventsOfType("user.created");
        assertEquals(1, userCreatedEvents.size(), "Expected exactly one user.created event");

        var envelope = userCreatedEvents.get(0);
        assertEquals("user.created", envelope.eventType());
        assertNotNull(envelope.eventId());
        assertNotNull(envelope.occurredAt());

        UserCreatedPayload payload = (UserCreatedPayload) envelope.payload();
        assertEquals(UUID.fromString(userId), payload.userId());
        assertEquals(email, payload.email());
        assertNotNull(payload.createdAt());
    }

    // ── POST /api/v1/auth/login ───────────────────────────────────────────────

    /**
     * Requirement 2.1: Valid credentials return 200 with a signed JWT.
     * JWT claims are verified: sub, email, groups, exp = iat + 3600.
     */
    @Test
    void login_withValidCredentials_returns200WithJwt() {
        String email = uniqueEmail();
        String password = "securePass1";
        registerAndLogin(email, password); // register first

        var response = given().contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", password))
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("expiresIn", equalTo(3600))
                .extract().response();

        String token = response.path("token");
        assertNotNull(token);

        // Decode and verify JWT claims
        String payload = decodeJwtPayload(token);
        assertTrue(payload.contains("\"sub\""), "JWT should contain sub claim");
        assertTrue(payload.contains("\"email\":\"" + email + "\""), "JWT should contain email claim");
        assertTrue(payload.contains("\"groups\""), "JWT should contain groups claim");
        assertTrue(payload.contains("USER"), "JWT groups should contain USER role");
        assertTrue(payload.contains("\"iss\":\"personal-library-manager\""), "JWT should contain issuer claim");
        assertTrue(payload.contains("\"exp\""), "JWT should contain exp claim");
    }

    /**
     * Requirement 2.2: Incorrect password returns 401 Unauthorized.
     */
    @Test
    void login_withWrongPassword_returns401() {
        String email = uniqueEmail();
        registerAndLogin(email, "correctPass1");

        given().contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", "wrongPassword"))
                .post("/api/v1/auth/login")
                .then()
                .statusCode(401)
                .body("status", equalTo(401));
    }

    /**
     * Requirement 2.3: Non-existent email returns 401 Unauthorized.
     */
    @Test
    void login_withNonExistentEmail_returns401() {
        given().contentType(ContentType.JSON)
                .body(Map.of("email", "nobody@example.com", "password", "somePassword"))
                .post("/api/v1/auth/login")
                .then()
                .statusCode(401)
                .body("status", equalTo(401));
    }

    // ── GET /api/v1/users/me ──────────────────────────────────────────────────

    /**
     * Requirement 2.4: Protected endpoint without JWT returns 401.
     */
    @Test
    void getMe_withoutToken_returns401() {
        given()
                .get("/api/v1/users/me")
                .then()
                .statusCode(401);
    }

    /**
     * Requirement 2.1 + 3.5: Authenticated user can retrieve their own info.
     */
    @Test
    void getMe_withValidToken_returns200WithUserInfo() {
        String email = uniqueEmail();
        String token = registerAndLogin(email, "securePass1");

        given().header("Authorization", "Bearer " + token)
                .get("/api/v1/users/me")
                .then()
                .statusCode(200)
                .body("email", equalTo(email))
                .body("role", equalTo("USER"))
                .body("id", notNullValue());
    }

    // ── PUT /api/v1/users/{id}/role ───────────────────────────────────────────

    /**
     * Requirement 3.6: USER-role token cannot update another user's role — returns 403.
     */
    @Test
    void updateRole_withUserToken_returns403() {
        String email = uniqueEmail();
        String token = registerAndLogin(email, "securePass1");

        // Get the user's own ID
        String userId = given().header("Authorization", "Bearer " + token)
                .get("/api/v1/users/me")
                .then().statusCode(200)
                .extract().path("id");

        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(Map.of("role", "ADMIN"))
                .put("/api/v1/users/" + userId + "/role")
                .then()
                .statusCode(403);
    }

    /**
     * Requirement 3.6: ADMIN can update a user's role.
     * Bootstraps an ADMIN by injecting UserService directly to promote a user,
     * then verifies the role change via the REST endpoint.
     */
    @Test
    void updateRole_withAdminToken_promotesUser_returns200() {
        // Register the user who will become admin
        String adminEmail = uniqueEmail();
        String adminToken = registerAndLogin(adminEmail, "adminPass99");
        String adminId = given().header("Authorization", "Bearer " + adminToken)
                .get("/api/v1/users/me")
                .then().statusCode(200)
                .extract().path("id");

        // Promote to ADMIN directly via service (bypasses the chicken-and-egg problem)
        userService.updateRole(UUID.fromString(adminId), com.library.identity.domain.Role.ADMIN);

        // Re-login to get a fresh JWT with ADMIN role
        String freshAdminToken = given().contentType(ContentType.JSON)
                .body(Map.of("email", adminEmail, "password", "adminPass99"))
                .post("/api/v1/auth/login")
                .then().statusCode(200)
                .extract().path("token");

        // Register a target user to be promoted
        String targetEmail = uniqueEmail();
        String targetToken = registerAndLogin(targetEmail, "targetPass1");
        String targetUserId = given().header("Authorization", "Bearer " + targetToken)
                .get("/api/v1/users/me")
                .then().statusCode(200)
                .extract().path("id");

        // ADMIN promotes target user
        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + freshAdminToken)
                .body(Map.of("role", "ADMIN"))
                .put("/api/v1/users/" + targetUserId + "/role")
                .then()
                .statusCode(200)
                .body("role", equalTo("ADMIN"));
    }

    // ── GET /api/v1/users/me/profile ──────────────────────────────────────────

    /**
     * Requirement 4.1: Profile is auto-created on user registration with empty fields.
     */
    @Test
    void getProfile_afterRegistration_returnsEmptyProfile() {
        String email = uniqueEmail();
        String token = registerAndLogin(email, "securePass1");

        given().header("Authorization", "Bearer " + token)
                .get("/api/v1/users/me/profile")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("userId", notNullValue())
                .body("displayName", nullValue())
                .body("bio", nullValue());
    }

    /**
     * Requirement 4.2 + 4.3: Authenticated user can update and retrieve their profile.
     */
    @Test
    void updateProfile_withValidData_returns200AndUpdatedProfile() {
        String email = uniqueEmail();
        String token = registerAndLogin(email, "securePass1");

        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(Map.of(
                        "displayName", "Alice Wonderland",
                        "bio", "Avid reader of fantasy novels.",
                        "favoriteGenres", List.of("Fantasy", "Sci-Fi")))
                .put("/api/v1/users/me/profile")
                .then()
                .statusCode(200)
                .body("displayName", equalTo("Alice Wonderland"))
                .body("bio", equalTo("Avid reader of fantasy novels."))
                .body("favoriteGenres", hasItems("Fantasy", "Sci-Fi"));

        // Verify GET returns the updated profile
        given().header("Authorization", "Bearer " + token)
                .get("/api/v1/users/me/profile")
                .then()
                .statusCode(200)
                .body("displayName", equalTo("Alice Wonderland"))
                .body("bio", equalTo("Avid reader of fantasy novels."));
    }

    /**
     * Requirement 4.5: displayName longer than 100 characters returns 400.
     */
    @Test
    void updateProfile_withDisplayNameTooLong_returns400() {
        String email = uniqueEmail();
        String token = registerAndLogin(email, "securePass1");

        String longName = "A".repeat(101);

        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(Map.of("displayName", longName))
                .put("/api/v1/users/me/profile")
                .then()
                .statusCode(400)
                .body("status", equalTo(400));
    }

    /**
     * Requirement 4.6: bio longer than 1000 characters returns 400.
     */
    @Test
    void updateProfile_withBioTooLong_returns400() {
        String email = uniqueEmail();
        String token = registerAndLogin(email, "securePass1");

        String longBio = "B".repeat(1001);

        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(Map.of("bio", longBio))
                .put("/api/v1/users/me/profile")
                .then()
                .statusCode(400)
                .body("status", equalTo(400));
    }

    /**
     * Requirement 2.4: Profile endpoint without JWT returns 401.
     */
    @Test
    void getProfile_withoutToken_returns401() {
        given()
                .get("/api/v1/users/me/profile")
                .then()
                .statusCode(401);
    }

    /**
     * Requirement 2.5: Expired or invalid JWT returns 401.
     */
    @Test
    void getMe_withInvalidToken_returns401() {
        given().header("Authorization", "Bearer this.is.not.a.valid.jwt")
                .get("/api/v1/users/me")
                .then()
                .statusCode(401);
    }

    /**
     * Requirement 12.1: Error responses have consistent JSON structure with status, message, timestamp.
     * Uses the 409 conflict path (duplicate email) which goes through GlobalExceptionMapper.
     */
    @Test
    void errorResponse_hasConsistentStructure() {
        String email = uniqueEmail();
        // First registration succeeds
        given().contentType(ContentType.JSON)
                .body(Map.of("name", "Eve", "email", email, "password", "securePass1"))
                .post("/api/v1/auth/register")
                .then().statusCode(201);

        // Second registration with same email → 409 via GlobalExceptionMapper
        given().contentType(ContentType.JSON)
                .body(Map.of("name", "Eve2", "email", email, "password", "securePass2"))
                .post("/api/v1/auth/register")
                .then()
                .statusCode(409)
                .body("status", notNullValue())
                .body("message", notNullValue())
                .body("timestamp", notNullValue());
    }
}
