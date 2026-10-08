package dev.craftboard.history.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

import dev.craftboard.history.HistoryService;
import dev.craftboard.history.Period;
import dev.craftboard.history.SnapshotPoint;
import dev.craftboard.history.StatSnapshot;
import dev.craftboard.history.StatSnapshotRepository;
import dev.craftboard.player.PlayerRepository;
import dev.craftboard.stats.PlayerStats;
import dev.craftboard.stats.StatsService;
import dev.craftboard.stats.StatsUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoryServiceImpl implements HistoryService {

    // Enough for a chart a few hundred pixels wide. A regular player passes it in days.
    private static final int MAX_HISTORY_POINTS = 500;

    private final StatsService statsService;
    private final PlayerRepository players;
    private final StatSnapshotRepository snapshots;
    private final TransactionTemplate transaction;

    // Stats events fired while the context is still starting are covered by the backfill.
    private volatile boolean ready;

    @Override
    public List<PlayerStats> ranking(Period period) {
        if (period == Period.ALL) {
            return statsService.all();
        }
        var from = Instant.now().minus(period.length());
        // Players tracked only after `from` fall back to their first snapshot.
        Map<UUID, StatSnapshot> baselines = new HashMap<>();
        snapshots.findFirstPerPlayerAfter(from).forEach(s -> baselines.put(s.getPlayerUuid(), s));
        snapshots.findLatestPerPlayerAtOrBefore(from).forEach(s -> baselines.put(s.getPlayerUuid(), s));

        return statsService.all().stream()
                .filter(current -> baselines.containsKey(current.uuid()))
                .map(current -> gainedSince(current, baselines.get(current.uuid())))
                .filter(gained -> gained.deaths() > 0 || gained.playTimeTicks() > 0)
                .sorted(Comparator.comparingLong(PlayerStats::playTimeTicks).reversed())
                .toList();
    }

    @Override
    public List<SnapshotPoint> history(UUID uuid, Instant from, Instant to) {
        var start = from != null ? from : snapshots.findFirstByPlayerUuidOrderByCapturedAtAscIdAsc(uuid)
                .map(StatSnapshot::getCapturedAt)
                .orElse(null);
        if (start == null) {
            return List.of();
        }
        var end = to != null ? to : Instant.now();
        var rows = snapshots.countByPlayerUuidAndCapturedAtBetween(uuid, start, end) <= MAX_HISTORY_POINTS
                ? snapshots.findByPlayerUuidAndCapturedAtBetweenOrderByCapturedAtAscIdAsc(uuid, start, end)
                : snapshots.findDownsampled(uuid, start, end, start.getEpochSecond(),
                        Math.max(1, Duration.between(start, end).toSeconds() / (double) MAX_HISTORY_POINTS));
        return rows.stream()
                .map(s -> new SnapshotPoint(s.getCapturedAt(), s.getDeaths(), s.getPlayTimeTicks()))
                .toList();
    }

    @EventListener(ApplicationReadyEvent.class)
    public synchronized void backfill() {
        // On an empty database the current totals are the starting point. After that, a player
        // seen for the first time started from zero, so their whole total belongs to the period.
        boolean initial = snapshots.count() == 0;
        statsService.all().forEach(stats -> record(stats, initial));
        ready = true;
    }

    @EventListener
    public synchronized void onStatsUpdated(StatsUpdatedEvent event) {
        if (ready) {
            record(event.stats(), false);
        }
    }

    // History is best effort: a database hiccup must never break the live dashboard.
    private void record(PlayerStats stats, boolean initial) {
        try {
            transaction.executeWithoutResult(status -> save(stats, initial));
        } catch (DataAccessException | TransactionException e) {
            log.warn("Could not record snapshot for {}: {}", stats.uuid(), e.getMessage());
        }
    }

    private void save(PlayerStats stats, boolean initial) {
        var now = Instant.now();
        // The session tracker may have created the player already, so the first snapshot is what
        // tells a newcomer apart, not the player row.
        var player = players.findOrCreate(stats.uuid(), stats.name(), now);
        player.setName(stats.name());

        var latest = snapshots.findFirstByPlayerUuidOrderByCapturedAtDescIdDesc(stats.uuid());
        if (latest.isEmpty() && !initial) {
            latest = Optional.of(snapshots.save(new StatSnapshot(stats.uuid(), 0, 0, now)));
        }
        if (latest.isPresent() && sameValues(latest.get(), stats)) {
            return;
        }
        snapshots.save(new StatSnapshot(stats.uuid(), stats.deaths(), stats.playTimeTicks(), now));
        player.setLastSeen(now);
    }

    private static PlayerStats gainedSince(PlayerStats current, StatSnapshot baseline) {
        // Counters go back after a world reset, which should read as zero, not negative.
        return new PlayerStats(
                current.uuid(),
                current.name(),
                Math.max(0, current.deaths() - baseline.getDeaths()),
                Math.max(0, current.playTimeTicks() - baseline.getPlayTimeTicks()),
                current.updatedAt());
    }

    private static boolean sameValues(StatSnapshot snapshot, PlayerStats stats) {
        return snapshot.getDeaths() == stats.deaths() && snapshot.getPlayTimeTicks() == stats.playTimeTicks();
    }
}
