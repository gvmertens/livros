package com.library.infrastructure;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Map;

/**
 * Shared Testcontainers PostgreSQL resource for all @QuarkusTest integration tests.
 *
 * <p>Starts a single PostgreSQL container once per test suite and injects the
 * datasource URL, username, and password into Quarkus configuration so that
 * Flyway and Hibernate ORM connect to the container instead of the default URL.
 *
 * <p>Usage:
 * <pre>{@code
 * @QuarkusTest
 * @QuarkusTestResource(PostgresTestResource.class)
 * class MyIntegrationTest { ... }
 * }</pre>
 */
public class PostgresTestResource implements QuarkusTestResourceLifecycleManager {

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("library_test")
                    .withUsername("test")
                    .withPassword("test");

    @Override
    public Map<String, String> start() {
        POSTGRES.start();
        return Map.of(
                "quarkus.datasource.jdbc.url", POSTGRES.getJdbcUrl(),
                "quarkus.datasource.username", POSTGRES.getUsername(),
                "quarkus.datasource.password", POSTGRES.getPassword()
        );
    }

    @Override
    public void stop() {
        POSTGRES.stop();
    }
}
