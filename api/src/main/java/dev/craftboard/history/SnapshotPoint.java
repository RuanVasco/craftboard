package dev.craftboard.history;

import java.time.Instant;

/** The extra counters are null on points captured before they were tracked. */
public record SnapshotPoint(
        Instant capturedAt,
        long deaths,
        long playTimeTicks,
        Long mobKills,
        Long playerKills,
        Long distanceCm,
        Long blocksMined) {

    public static SnapshotPoint of(StatSnapshot s) {
        return new SnapshotPoint(s.getCapturedAt(), s.getDeaths(), s.getPlayTimeTicks(), s.getMobKills(),
                s.getPlayerKills(), s.getDistanceCm(), s.getBlocksMined());
    }
}
