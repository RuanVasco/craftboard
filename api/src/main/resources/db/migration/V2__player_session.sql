-- Only filled when RCON is enabled, since the server files alone cannot tell who is connected.
CREATE TABLE player_session (
    id          BIGSERIAL PRIMARY KEY,
    player_uuid UUID NOT NULL REFERENCES player (uuid),
    joined_at   TIMESTAMPTZ NOT NULL,
    left_at     TIMESTAMPTZ
);
CREATE INDEX idx_player_session_player_joined ON player_session (player_uuid, joined_at);
CREATE INDEX idx_player_session_open ON player_session (player_uuid) WHERE left_at IS NULL;
