package dev.craftboard.activity;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import dev.craftboard.history.StatSnapshot;

public interface PlayTimeGainRepository extends Repository<StatSnapshot, Long> {

    // Capped by the wall clock between the two snapshots: a newcomer's first real snapshot comes
    // right after a zero baseline, and their whole total must not land in a single hour.
    @Query(value = """
            SELECT player_uuid AS playerUuid,
                   CAST(EXTRACT(EPOCH FROM captured_at) AS DOUBLE PRECISION) AS capturedAtEpoch,
                   CAST(LEAST(play_time_ticks - prev_ticks,
                              EXTRACT(EPOCH FROM captured_at - prev_at) * 20) AS DOUBLE PRECISION) AS ticks
            FROM (
                SELECT player_uuid, captured_at, play_time_ticks,
                       LAG(play_time_ticks) OVER w AS prev_ticks,
                       LAG(captured_at) OVER w AS prev_at
                FROM stat_snapshot
                WHERE captured_at >= :from
                WINDOW w AS (PARTITION BY player_uuid ORDER BY captured_at, id)) s
            WHERE play_time_ticks > prev_ticks""", nativeQuery = true)
    List<PlayTimeGain> findSince(Instant from);
}
