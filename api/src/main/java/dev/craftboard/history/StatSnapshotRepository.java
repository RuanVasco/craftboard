package dev.craftboard.history;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StatSnapshotRepository extends JpaRepository<StatSnapshot, Long> {

    Optional<StatSnapshot> findFirstByPlayerUuidOrderByCapturedAtDescIdDesc(UUID playerUuid);

    List<StatSnapshot> findByPlayerUuidAndCapturedAtBetweenOrderByCapturedAtAscIdAsc(
            UUID playerUuid, Instant from, Instant to);

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
