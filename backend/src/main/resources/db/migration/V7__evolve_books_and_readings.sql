-- Evolves the existing catalog without replacing book or reading identifiers.
-- Legacy columns remain available during the API transition and can only be
-- removed after the backfill has been validated in production-like data.

ALTER TABLE books
    ALTER COLUMN isbn DROP NOT NULL,
    ALTER COLUMN author_id DROP NOT NULL,
    ALTER COLUMN publisher_id DROP NOT NULL,
    ADD COLUMN google_books_id VARCHAR(255),
    ADD COLUMN subtitle VARCHAR(500),
    ADD COLUMN description TEXT,
    ADD COLUMN isbn_10 VARCHAR(10),
    ADD COLUMN isbn_13 VARCHAR(13),
    ADD COLUMN language VARCHAR(16),
    ADD COLUMN published_date VARCHAR(32),
    ADD COLUMN page_count INTEGER,
    ADD COLUMN thumbnail_url TEXT,
    ADD COLUMN large_cover_url TEXT,
    ADD COLUMN info_url TEXT,
    ADD COLUMN print_type VARCHAR(50),
    ADD COLUMN data_source VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
    ADD COLUMN metadata_updated_at TIMESTAMPTZ,
    ADD COLUMN normalized_title VARCHAR(500),
    ADD COLUMN normalized_primary_author VARCHAR(255),
    ADD COLUMN normalized_publisher VARCHAR(255),
    ADD CONSTRAINT chk_books_page_count CHECK (page_count IS NULL OR page_count >= 0),
    ADD CONSTRAINT chk_books_data_source CHECK (data_source IN ('GOOGLE_BOOKS', 'MANUAL'));

-- Existing ISBN values are preserved verbatim in books.isbn. Only values that
-- clearly match ISBN-10/13 after removing spaces and hyphens are copied.
UPDATE books
SET isbn_10 = CASE
        WHEN length(regexp_replace(isbn, '[ -]', '', 'g')) = 10
            THEN upper(regexp_replace(isbn, '[ -]', '', 'g'))
        ELSE NULL
    END,
    isbn_13 = CASE
        WHEN length(regexp_replace(isbn, '[ -]', '', 'g')) = 13
            THEN regexp_replace(isbn, '[ -]', '', 'g')
        ELSE NULL
    END,
    normalized_title = lower(trim(regexp_replace(title, '\s+', ' ', 'g'))),
    metadata_updated_at = COALESCE(updated_at, now());

UPDATE books b
SET normalized_primary_author = lower(trim(regexp_replace(a.name, '\s+', ' ', 'g')))
FROM authors a
WHERE b.author_id = a.id;

UPDATE books b
SET normalized_publisher = lower(trim(regexp_replace(p.name, '\s+', ' ', 'g')))
FROM publishers p
WHERE b.publisher_id = p.id;

CREATE TABLE book_authors (
    book_id UUID NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES authors(id),
    author_order INTEGER NOT NULL DEFAULT 0 CHECK (author_order >= 0),
    PRIMARY KEY (book_id, author_id)
);

INSERT INTO book_authors (book_id, author_id, author_order)
SELECT id, author_id, 0
FROM books
WHERE author_id IS NOT NULL
ON CONFLICT DO NOTHING;

CREATE TABLE book_external_categories (
    book_id UUID NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    category VARCHAR(255) NOT NULL,
    category_order INTEGER NOT NULL DEFAULT 0 CHECK (category_order >= 0),
    PRIMARY KEY (book_id, category)
);

ALTER TABLE readings
    ADD COLUMN favorite BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN notes TEXT;

CREATE UNIQUE INDEX uq_books_google_books_id
    ON books (google_books_id)
    WHERE google_books_id IS NOT NULL;
CREATE INDEX idx_books_isbn_10 ON books (isbn_10) WHERE isbn_10 IS NOT NULL;
CREATE INDEX idx_books_isbn_13 ON books (isbn_13) WHERE isbn_13 IS NOT NULL;
CREATE INDEX idx_books_normalized_title ON books (normalized_title);
CREATE INDEX idx_books_language ON books (language) WHERE language IS NOT NULL;
CREATE INDEX idx_book_authors_author_id ON book_authors (author_id);
