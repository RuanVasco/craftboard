package dev.craftboard.stats;

import java.time.Instant;
import java.util.UUID;

public record PlayerStats(UUID uuid, String name, long deaths, long playTimeTicks, Instant updatedAt) {
}
