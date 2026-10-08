-- 36 fits the uuid string used as a fallback name when usercache.json does not know the player.
CREATE TABLE player (
    uuid        UUID PRIMARY KEY,
    name        VARCHAR(36) NOT NULL,
    first_seen  TIMESTAMPTZ NOT NULL,
    last_seen   TIMESTAMPTZ NOT NULL
);

CREATE TABLE stat_snapshot (
    id              BIGSERIAL PRIMARY KEY,
    player_uuid     UUID NOT NULL REFERENCES player (uuid),
    deaths          BIGINT NOT NULL,
    play_time_ticks BIGINT NOT NULL,
    captured_at     TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_stat_snapshot_player_time ON stat_snapshot (player_uuid, captured_at);
