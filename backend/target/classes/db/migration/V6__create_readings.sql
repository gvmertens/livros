CREATE TABLE readings (
    id          UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID           NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    book_id     UUID           NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    status      VARCHAR(20)    NOT NULL DEFAULT 'WANT_TO_READ'
                    CHECK (status IN ('WANT_TO_READ','READING','FINISHED','ABANDONED')),
    rating      NUMERIC(4,2)   CHECK (rating >= 0.0 AND rating <= 10.0),
    review      TEXT,
    started_at  TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uq_readings_user_book UNIQUE (user_id, book_id),
    CONSTRAINT chk_finished_after_started
        CHECK (finished_at IS NULL OR started_at IS NULL OR finished_at >= started_at)
);
CREATE INDEX idx_readings_user_id ON readings(user_id);
CREATE INDEX idx_readings_status  ON readings(user_id, status);
