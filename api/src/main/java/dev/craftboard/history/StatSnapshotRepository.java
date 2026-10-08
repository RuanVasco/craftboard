package dev.craftboard.history;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StatSnapshotRepository extends JpaRepository<StatSnapshot, Long> {

    Optional<StatSnapshot> findFirstByPlayerUuidOrderByCapturedAtDescIdDesc(UUID playerUuid);

    Optional<StatSnapshot> findFirstByPlayerUuidOrderByCapturedAtAscIdAsc(UUID playerUuid);

    long countByPlayerUuidAndCapturedAtBetween(UUID playerUuid, Instant from, Instant to);

    List<StatSnapshot> findByPlayerUuidAndCapturedAtBetweenOrderByCapturedAtAscIdAsc(
            UUID playerUuid, Instant from, Instant to);

    // Last snapshot of each time bucket. Values are cumulative, so it stands for the whole bucket.
    @Query(value = """
            SELECT DISTINCT ON (bucket) id, player_uuid, deaths, play_time_ticks, captured_at FROM (
                SELECT *, floor((extract(epoch FROM captured_at) - :fromEpoch) / :bucketSeconds) AS bucket
                FROM stat_snapshot
                WHERE player_uuid = :playerUuid AND captured_at BETWEEN :from AND :to) b
            ORDER BY bucket, captured_at DESC, id DESC""", nativeQuery = true)
    List<StatSnapshot> findDownsampled(
            UUID playerUuid, Instant from, Instant to, double fromEpoch, double bucketSeconds);

    // One index lookup per player instead of scanning a table that only grows.
    @Query(value = """
            SELECT s.* FROM player p CROSS JOIN LATERAL (
                SELECT * FROM stat_snapshot
                WHERE player_uuid = p.uuid AND captured_at <= :at
                ORDER BY captured_at DESC, id DESC LIMIT 1) s""", nativeQuery = true)
    List<StatSnapshot> findLatestPerPlayerAtOrBefore(Instant at);

    @Query(value = """
            SELECT s.* FROM player p CROSS JOIN LATERAL (
                SELECT * FROM stat_snapshot
                WHERE player_uuid = p.uuid AND captured_at > :at
                ORDER BY captured_at, id LIMIT 1) s""", nativeQuery = true)
    List<StatSnapshot> findFirstPerPlayerAfter(Instant at);
}
