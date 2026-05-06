package com.library.catalog.infrastructure;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that Flyway migrations V3–V5 (authors, publishers, books) apply cleanly
 * against a real PostgreSQL instance via Testcontainers.
 *
 * <p>This test starts a PostgreSQL container, runs Flyway migrations V1–V5,
 * and then asserts that the expected tables and constraints exist.
 */
@Testcontainers
class FlywayMigrationV3ToV5Test {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("library_test")
            .withUsername("test")
            .withPassword("test");

    /**
     * Runs all migrations V1–V5 via Flyway and verifies the resulting schema.
     */
    @Test
    void migrations_V1_to_V5_applyCleanly() throws Exception {
        // Run Flyway programmatically against the Testcontainers PostgreSQL instance
        org.flywaydb.core.Flyway flyway = org.flywaydb.core.Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load();

        flyway.migrate();
        // Verify the schema history shows at least 5 applied migrations (V1–V5)
        // migrationsExecuted may be 0 if migrations were already applied by a prior test
        var info = flyway.info();
        long appliedCount = java.util.Arrays.stream(info.applied())
                .filter(m -> m.getState().isApplied())
                .count();
        assertTrue(appliedCount >= 5,
                "Expected at least 5 migrations (V1–V5) to be applied, got: " + appliedCount);

        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {

            // ── V3: authors table ─────────────────────────────────────────────
            assertTableExists(conn, "authors");
            assertColumnExists(conn, "authors", "id");
            assertColumnExists(conn, "authors", "name");
            assertColumnExists(conn, "authors", "created_at");
            assertColumnExists(conn, "authors", "updated_at");

            // ── V4: publishers table ──────────────────────────────────────────
            assertTableExists(conn, "publishers");
            assertColumnExists(conn, "publishers", "id");
            assertColumnExists(conn, "publishers", "name");
            assertColumnExists(conn, "publishers", "created_at");
            assertColumnExists(conn, "publishers", "updated_at");

            // ── V5: books table ───────────────────────────────────────────────
            assertTableExists(conn, "books");
            assertColumnExists(conn, "books", "id");
            assertColumnExists(conn, "books", "isbn");
            assertColumnExists(conn, "books", "title");
            assertColumnExists(conn, "books", "author_id");
            assertColumnExists(conn, "books", "publisher_id");
            assertColumnExists(conn, "books", "created_at");
            assertColumnExists(conn, "books", "updated_at");

            // ── V5: ISBN unique constraint ────────────────────────────────────
            assertUniqueConstraintExists(conn, "books", "isbn");

            // ── V5: FK → authors ──────────────────────────────────────────────
            assertForeignKeyExists(conn, "books", "author_id", "authors");

            // ── V5: FK → publishers ───────────────────────────────────────────
            assertForeignKeyExists(conn, "books", "publisher_id", "publishers");

            // ── V5: GIN index on title ────────────────────────────────────────
            assertIndexExists(conn, "idx_books_title");
        }
    }

    @Test
    void authors_table_canInsertAndQuery() throws Exception {
        // Ensure Flyway has run (idempotent — won't re-run already applied migrations)
        org.flywaydb.core.Flyway flyway = org.flywaydb.core.Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();

        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement stmt = conn.createStatement()) {

            stmt.execute("INSERT INTO authors (name) VALUES ('J.R.R. Tolkien')");
            ResultSet rs = stmt.executeQuery("SELECT name FROM authors WHERE name = 'J.R.R. Tolkien'");
            assertTrue(rs.next(), "Expected to find inserted author");
            assertEquals("J.R.R. Tolkien", rs.getString("name"));
        }
    }

    @Test
    void publishers_table_canInsertAndQuery() throws Exception {
        org.flywaydb.core.Flyway flyway = org.flywaydb.core.Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();

        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement stmt = conn.createStatement()) {

            stmt.execute("INSERT INTO publishers (name) VALUES ('Allen & Unwin')");
            ResultSet rs = stmt.executeQuery("SELECT name FROM publishers WHERE name = 'Allen & Unwin'");
            assertTrue(rs.next(), "Expected to find inserted publisher");
            assertEquals("Allen & Unwin", rs.getString("name"));
        }
    }

    @Test
    void books_table_canInsertWithForeignKeys() throws Exception {
        org.flywaydb.core.Flyway flyway = org.flywaydb.core.Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();

        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement stmt = conn.createStatement()) {

            // Insert author and publisher first
            stmt.execute("INSERT INTO authors (id, name) VALUES ('a0000000-0000-0000-0000-000000000001', 'Test Author')");
            stmt.execute("INSERT INTO publishers (id, name) VALUES ('b0000000-0000-0000-0000-000000000001', 'Test Publisher')");

            // Insert book referencing them
            stmt.execute("""
                    INSERT INTO books (isbn, title, author_id, publisher_id)
                    VALUES ('9780000000001', 'Test Book',
                            'a0000000-0000-0000-0000-000000000001',
                            'b0000000-0000-0000-0000-000000000001')
                    """);

            ResultSet rs = stmt.executeQuery("SELECT isbn, title FROM books WHERE isbn = '9780000000001'");
            assertTrue(rs.next(), "Expected to find inserted book");
            assertEquals("Test Book", rs.getString("title"));
        }
    }

    @Test
    void books_isbn_uniqueConstraint_rejectsInsertDuplicate() throws Exception {
        org.flywaydb.core.Flyway flyway = org.flywaydb.core.Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();

        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement stmt = conn.createStatement()) {

            stmt.execute("INSERT INTO authors (id, name) VALUES ('c0000000-0000-0000-0000-000000000001', 'Author C')");
            stmt.execute("INSERT INTO publishers (id, name) VALUES ('d0000000-0000-0000-0000-000000000001', 'Publisher D')");
            stmt.execute("""
                    INSERT INTO books (isbn, title, author_id, publisher_id)
                    VALUES ('9780000000002', 'Book One',
                            'c0000000-0000-0000-0000-000000000001',
                            'd0000000-0000-0000-0000-000000000001')
                    """);

            // Duplicate ISBN should fail
            assertThrows(Exception.class, () ->
                    stmt.execute("""
                            INSERT INTO books (isbn, title, author_id, publisher_id)
                            VALUES ('9780000000002', 'Book Two',
                                    'c0000000-0000-0000-0000-000000000001',
                                    'd0000000-0000-0000-0000-000000000001')
                            """),
                    "Expected unique constraint violation for duplicate ISBN");
        }
    }

    @Test
    void books_foreignKey_rejectsInvalidAuthorId() throws Exception {
        org.flywaydb.core.Flyway flyway = org.flywaydb.core.Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();

        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement stmt = conn.createStatement()) {

            stmt.execute("INSERT INTO publishers (id, name) VALUES ('e0000000-0000-0000-0000-000000000001', 'Publisher E')");

            // Non-existent author_id should fail FK constraint
            assertThrows(Exception.class, () ->
                    stmt.execute("""
                            INSERT INTO books (isbn, title, author_id, publisher_id)
                            VALUES ('9780000000003', 'Book Three',
                                    'f0000000-0000-0000-0000-000000000099',
                                    'e0000000-0000-0000-0000-000000000001')
                            """),
                    "Expected FK violation for non-existent author_id");
        }
    }

    // ── assertion helpers ─────────────────────────────────────────────────────

    private void assertTableExists(Connection conn, String tableName) throws Exception {
        try (ResultSet rs = conn.getMetaData().getTables(null, "public", tableName, new String[]{"TABLE"})) {
            assertTrue(rs.next(), "Table '" + tableName + "' should exist");
        }
    }

    private void assertColumnExists(Connection conn, String tableName, String columnName) throws Exception {
        try (ResultSet rs = conn.getMetaData().getColumns(null, "public", tableName, columnName)) {
            assertTrue(rs.next(), "Column '" + columnName + "' should exist in table '" + tableName + "'");
        }
    }

    private void assertUniqueConstraintExists(Connection conn, String tableName, String columnName) throws Exception {
        try (ResultSet rs = conn.getMetaData().getIndexInfo(null, "public", tableName, true, false)) {
            boolean found = false;
            while (rs.next()) {
                String col = rs.getString("COLUMN_NAME");
                if (columnName.equalsIgnoreCase(col)) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "Unique constraint on '" + columnName + "' in table '" + tableName + "' should exist");
        }
    }

    private void assertForeignKeyExists(Connection conn, String tableName, String columnName, String referencedTable) throws Exception {
        try (ResultSet rs = conn.getMetaData().getImportedKeys(null, "public", tableName)) {
            boolean found = false;
            while (rs.next()) {
                String fkCol = rs.getString("FKCOLUMN_NAME");
                String pkTable = rs.getString("PKTABLE_NAME");
                if (columnName.equalsIgnoreCase(fkCol) && referencedTable.equalsIgnoreCase(pkTable)) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "FK from '" + tableName + "." + columnName + "' → '" + referencedTable + "' should exist");
        }
    }

    private void assertIndexExists(Connection conn, String indexName) throws Exception {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT indexname FROM pg_indexes WHERE indexname = '" + indexName + "'")) {
            assertTrue(rs.next(), "Index '" + indexName + "' should exist");
        }
    }
}
