-- Log lines carry only a time of day, so the same line read again after a restart maps to the same row.
CREATE TABLE feed_event (
    id          BIGSERIAL PRIMARY KEY,
    type        VARCHAR(16) NOT NULL,
    player      VARCHAR(36),
    detail      TEXT,
    message     TEXT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    UNIQUE (occurred_at, message)
);
