package dev.craftboard.activity;

import java.time.Instant;

/**
 * When a player was first and last seen by craftboard. Session numbers come from RCON polling and
 * are null without it.
 */
public record PlayerSummary(
        Instant firstSeen,
        Instant lastSeen,
        Integer sessions,
        Long longestSessionSeconds,
        Long averageSessionSeconds) {
}
