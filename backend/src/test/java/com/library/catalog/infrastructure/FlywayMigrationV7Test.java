package com.library.catalog.infrastructure;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class FlywayMigrationV7Test {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("library_v7_test")
            .withUsername("test")
            .withPassword("test");

    @BeforeEach
    void resetSchema() {
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .cleanDisabled(false)
                .load()
                .clean();
    }

    @Test
    void migrationPreservesLegacyDataAndBackfillsNewModel() throws Exception {
        migrateTo("6");

        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO users (id, name, email, password_hash)
                    VALUES ('10000000-0000-0000-0000-000000000001', 'Reader', 'reader@example.com', 'hash')
                    """);
            statement.execute("""
                    INSERT INTO authors (id, name)
                    VALUES ('20000000-0000-0000-0000-000000000001', 'Alex  Michaelides')
                    """);
            statement.execute("""
                    INSERT INTO publishers (id, name)
                    VALUES ('30000000-0000-0000-0000-000000000001', 'Harper  Collins')
                    """);
            statement.execute("""
                    INSERT INTO books (id, isbn, title, author_id, publisher_id)
                    VALUES ('40000000-0000-0000-0000-000000000001', '9780000000001',
                            'A  Paciente Silenciosa',
                            '20000000-0000-0000-0000-000000000001',
                            '30000000-0000-0000-0000-000000000001')
                    """);
            statement.execute("""
                    INSERT INTO readings (id, user_id, book_id, status, rating, review)
                    VALUES ('50000000-0000-0000-0000-000000000001',
                            '10000000-0000-0000-0000-000000000001',
                            '40000000-0000-0000-0000-000000000001',
                            'FINISHED', 9.0, 'Excelente')
                    """);
        }

        migrateTo(null);

        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            try (ResultSet books = statement.executeQuery("""
                    SELECT isbn, isbn_13, normalized_title, normalized_primary_author,
                           normalized_publisher, data_source, metadata_updated_at
                    FROM books
                    WHERE id = '40000000-0000-0000-0000-000000000001'
                    """)) {
                assertTrue(books.next());
                assertEquals("9780000000001", books.getString("isbn"));
                assertEquals("9780000000001", books.getString("isbn_13"));
                assertEquals("a paciente silenciosa", books.getString("normalized_title"));
                assertEquals("alex michaelides", books.getString("normalized_primary_author"));
                assertEquals("harper collins", books.getString("normalized_publisher"));
                assertEquals("MANUAL", books.getString("data_source"));
                assertNotNull(books.getTimestamp("metadata_updated_at"));
            }

            try (ResultSet authors = statement.executeQuery("""
                    SELECT author_id, author_order FROM book_authors
                    WHERE book_id = '40000000-0000-0000-0000-000000000001'
                    """)) {
                assertTrue(authors.next());
                assertEquals("20000000-0000-0000-0000-000000000001", authors.getString("author_id"));
                assertEquals(0, authors.getInt("author_order"));
                assertFalse(authors.next());
            }

            try (ResultSet reading = statement.executeQuery("""
                    SELECT status, rating, review, favorite, notes FROM readings
                    WHERE id = '50000000-0000-0000-0000-000000000001'
                    """)) {
                assertTrue(reading.next());
                assertEquals("FINISHED", reading.getString("status"));
                assertEquals("9.00", reading.getBigDecimal("rating").setScale(2).toPlainString());
                assertEquals("Excelente", reading.getString("review"));
                assertFalse(reading.getBoolean("favorite"));
                assertNull(reading.getString("notes"));
            }
        }
    }

    @Test
    void evolvedSchemaAcceptsBooksWithoutIsbnAuthorOrPublisher() throws Exception {
        migrateTo(null);

        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO books (id, title, google_books_id, data_source)
                    VALUES ('60000000-0000-0000-0000-000000000001',
                            'Volume incompleto', 'google-volume-1', 'GOOGLE_BOOKS')
                    """);

            ResultSet result = statement.executeQuery("""
                    SELECT google_books_id, isbn, author_id, publisher_id
                    FROM books WHERE id = '60000000-0000-0000-0000-000000000001'
                    """);
            assertTrue(result.next());
            assertEquals("google-volume-1", result.getString("google_books_id"));
            assertNull(result.getString("isbn"));
            assertNull(result.getString("author_id"));
            assertNull(result.getString("publisher_id"));
        }
    }

    @Test
    void googleBooksIdConstraintRejectsDuplicates() throws Exception {
        migrateTo(null);

        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("INSERT INTO books (title, google_books_id) VALUES ('First', 'duplicate-volume')");
            assertThrows(Exception.class, () ->
                    statement.execute("INSERT INTO books (title, google_books_id) VALUES ('Second', 'duplicate-volume')"));
        }
    }

    private void migrateTo(String target) {
        var configuration = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration");
        if (target != null) {
            configuration.target(MigrationVersion.fromVersion(target));
        }
        configuration.load().migrate();
    }

    private Connection connection() throws Exception {
        return DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }
}
