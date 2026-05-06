package com.library.shared;

import com.library.infrastructure.PostgresTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration test for the SmallRye Health endpoint.
 *
 * <p>Verifies that {@code GET /q/health} reports the application status and
 * database connection status as UP when a real PostgreSQL instance is available.
 *
 * <p>Requirements: 12.5
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class HealthCheckIT {

    /**
     * Requirement 12.5: GET /q/health returns 200 with application and DB status.
     *
     * <p>SmallRye Health aggregates all registered health checks. The response
     * contains a top-level {@code status} field and a {@code checks} array.
     * With a live PostgreSQL container, both the application liveness and the
     * datasource readiness checks should report UP.
     */
    @Test
    void health_withLiveDatabase_returnsUp() {
        given()
                .get("/q/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"))
                .body("checks", not(empty()));
    }

    /**
     * Requirement 12.5: GET /q/health/live returns 200 — application is alive.
     */
    @Test
    void healthLive_returns200Up() {
        given()
                .get("/q/health/live")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }

    /**
     * Requirement 12.5: GET /q/health/ready returns 200 — datasource is ready.
     *
     * <p>The datasource readiness check confirms the PostgreSQL connection is
     * established and Flyway migrations have completed successfully.
     */
    @Test
    void healthReady_withLiveDatabase_returns200Up() {
        given()
                .get("/q/health/ready")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"))
                .body("checks.name", hasItem("Database connections health check"));
    }
}
