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

    @Query(value = """
            SELECT DISTINCT ON (player_uuid) * FROM stat_snapshot
            WHERE captured_at <= :at
            ORDER BY player_uuid, captured_at DESC, id DESC""", nativeQuery = true)
    List<StatSnapshot> findLatestPerPlayerAtOrBefore(Instant at);

    @Query(value = """
            SELECT DISTINCT ON (player_uuid) * FROM stat_snapshot
            ORDER BY player_uuid, captured_at, id""", nativeQuery = true)
    List<StatSnapshot> findEarliestPerPlayer();
}
