package dev.craftboard.player;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PlayerRepository extends JpaRepository<Player, UUID> {

    // The stats watcher and the RCON poll may meet the same new player at once.
    @Modifying
    @Query(value = """
            INSERT INTO player (uuid, name, first_seen, last_seen) VALUES (:uuid, :name, :seenAt, :seenAt)
            ON CONFLICT (uuid) DO NOTHING""", nativeQuery = true)
    void insertIfAbsent(UUID uuid, String name, Instant seenAt);

    default Player findOrCreate(UUID uuid, String name, Instant seenAt) {
        insertIfAbsent(uuid, name, seenAt);
        return findById(uuid).orElseThrow();
    }
}
