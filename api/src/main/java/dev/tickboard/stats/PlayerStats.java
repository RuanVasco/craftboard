package dev.tickboard.stats;

import java.time.Instant;
import java.util.UUID;

public record PlayerStats(UUID uuid, String name, long deaths, long playTimeTicks, Instant updatedAt) {

    private static final int TICKS_PER_SECOND = 20;

    public long playTimeSeconds() {
        return playTimeTicks / TICKS_PER_SECOND;
    }
}
