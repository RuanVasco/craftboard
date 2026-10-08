package dev.craftboard.world;

import java.time.Instant;

/**
 * World clock and weather as of the last save ({@code capturedAt}). Any field the running game
 * version does not write is null.
 *
 * @param day        in game day, starting at 1
 * @param timeOfDay  ticks into the current day, 0 is sunrise and 13000 is nightfall
 */
public record WorldInfo(
        String version,
        Long day,
        Long timeOfDay,
        boolean raining,
        boolean thundering,
        Instant capturedAt) {
}
