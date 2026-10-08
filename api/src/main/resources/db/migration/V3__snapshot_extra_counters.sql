-- Null on rows captured before these counters were tracked, so a period never treats them as zero.
ALTER TABLE stat_snapshot
    ADD COLUMN mob_kills    BIGINT,
    ADD COLUMN player_kills BIGINT,
    ADD COLUMN distance_cm  BIGINT,
    ADD COLUMN blocks_mined BIGINT;
