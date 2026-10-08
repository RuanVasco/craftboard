package dev.craftboard.history;

import java.time.Instant;
import java.util.UUID;

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

    private Instant capturedAt;

    public StatSnapshot(UUID playerUuid, long deaths, long playTimeTicks, Instant capturedAt) {
        this.playerUuid = playerUuid;
        this.deaths = deaths;
        this.playTimeTicks = playTimeTicks;
        this.capturedAt = capturedAt;
    }
}
