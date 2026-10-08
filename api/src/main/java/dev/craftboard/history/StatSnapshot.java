package dev.craftboard.history;

import java.time.Instant;
import java.util.UUID;

import dev.craftboard.stats.PlayerStats;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stat_snapshot")
@Getter
@NoArgsConstructor
public class StatSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID playerUuid;

    private long deaths;

    private long playTimeTicks;

    // Null on snapshots older than V3.
    private Long mobKills;

    private Long playerKills;

    private Long distanceCm;

    private Long blocksMined;

    private Instant capturedAt;

    public StatSnapshot(UUID playerUuid, long deaths, long playTimeTicks, Instant capturedAt) {
        this.playerUuid = playerUuid;
        this.deaths = deaths;
        this.playTimeTicks = playTimeTicks;
        this.capturedAt = capturedAt;
    }

    public StatSnapshot(PlayerStats stats, Instant capturedAt) {
        this(stats.uuid(), stats.deaths(), stats.playTimeTicks(), capturedAt);
        this.mobKills = stats.mobKills();
        this.playerKills = stats.playerKills();
        this.distanceCm = stats.distanceCm();
        this.blocksMined = stats.blocksMined();
    }

    public static StatSnapshot zero(UUID playerUuid, Instant capturedAt) {
        var snapshot = new StatSnapshot(playerUuid, 0, 0, capturedAt);
        snapshot.mobKills = 0L;
        snapshot.playerKills = 0L;
        snapshot.distanceCm = 0L;
        snapshot.blocksMined = 0L;
        return snapshot;
    }
}
