package dev.craftboard.history;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import dev.craftboard.stats.PlayerStats;

public interface HistoryService {

    /**
     * Current totals for {@link Period#ALL}, otherwise what each player gained inside the period.
     * Players that gained nothing in the period are left out.
     */
    List<PlayerStats> ranking(Period period);

    List<SnapshotPoint> history(UUID uuid, Instant from, Instant to);
}
