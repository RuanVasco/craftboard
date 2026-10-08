package dev.craftboard.history;

import java.time.Instant;

public record SnapshotPoint(Instant capturedAt, long deaths, long playTimeTicks) {
}
