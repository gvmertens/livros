CREATE TABLE books (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    isbn         VARCHAR(13)  NOT NULL UNIQUE,
    title        VARCHAR(500) NOT NULL,
    author_id    UUID         NOT NULL REFERENCES authors(id),
    publisher_id UUID         NOT NULL REFERENCES publishers(id),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_books_isbn      ON books(isbn);
CREATE INDEX idx_books_author_id ON books(author_id);
CREATE INDEX idx_books_title     ON books USING gin(to_tsvector('english', title));
