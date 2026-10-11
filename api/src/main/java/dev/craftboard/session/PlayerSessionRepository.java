package dev.craftboard.session;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PlayerSessionRepository extends JpaRepository<PlayerSession, Long> {

    List<PlayerSession> findByLeftAtIsNull();

    List<PlayerSession> findByPlayerUuid(UUID playerUuid);

    @Query(value = """
            SELECT player_uuid AS "playerUuid",
                   CAST(EXTRACT(EPOCH FROM COALESCE(left_at, now()) - joined_at) AS BIGINT) AS "seconds"
            FROM player_session
            ORDER BY 2 DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<Longest> findLongest();

    interface Longest {

        UUID getPlayerUuid();

        long getSeconds();
    }
}
