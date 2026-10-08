package dev.craftboard.activity.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.craftboard.IntegrationTest;
import dev.craftboard.config.MinecraftProperties;
import dev.craftboard.history.StatSnapshot;
import dev.craftboard.history.StatSnapshotRepository;
import dev.craftboard.player.OfflineUuid;
import dev.craftboard.player.Player;
import dev.craftboard.player.PlayerRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class ActivityServiceImplTest extends IntegrationTest {

    private static final UUID STEVE = OfflineUuid.of("Steve");
    private static final UUID ALEX = OfflineUuid.of("Alex");
    private static final long TICKS_PER_HOUR = 72_000;

    private final ActivityServiceImpl activityService;
    private final PlayerRepository players;
    private final StatSnapshotRepository snapshots;
    private final MinecraftProperties properties;

    @Test
    void bucketsPlayTimeGainsByHourAndDay() {
        // Both snapshots fall inside the same local hour, so the whole gain lands in one cell.
        var start = ZonedDateTime.now(properties.timezone()).minusHours(2).withMinute(0).withSecond(0).withNano(0);
        players.save(new Player(STEVE, "Steve", start.toInstant()));
        snapshots.save(new StatSnapshot(STEVE, 0, 1000, start.toInstant()));
        snapshots.save(new StatSnapshot(STEVE, 0, 1000 + TICKS_PER_HOUR / 2, start.plusMinutes(45).toInstant()));

        var activity = activityService.server(7);

        assertThat(activity.daily()).hasSize(7);
        var day = activity.daily().stream().filter(d -> d.date().equals(start.toLocalDate())).findFirst().orElseThrow();
        assertThat(day.hours()).isCloseTo(0.5, within(1e-9));
        assertThat(day.players()).isEqualTo(1);
        var hourCell = activity.heatmap()[start.getDayOfWeek().getValue() - 1][start.getHour()];
        assertThat(hourCell).isCloseTo(0.5, within(1e-9));
    }

    @Test
    void capsAGainAtTheWallClockBetweenSnapshots() {
        var start = Instant.now().minus(Duration.ofHours(1));
        players.save(new Player(STEVE, "Steve", start));
        snapshots.save(new StatSnapshot(STEVE, 0, 0, start));
        snapshots.save(new StatSnapshot(STEVE, 0, 100 * TICKS_PER_HOUR, start.plus(Duration.ofMinutes(30))));

        var total = activityService.server(2).daily().stream().mapToDouble(d -> d.hours()).sum();

        assertThat(total).isCloseTo(0.5, within(1e-9));
    }

    @Test
    void playerActivityLeavesOthersOut() {
        var start = Instant.now().minus(Duration.ofHours(1));
        players.save(new Player(STEVE, "Steve", start));
        players.save(new Player(ALEX, "Alex", start));
        snapshots.save(new StatSnapshot(STEVE, 0, 0, start));
        snapshots.save(new StatSnapshot(STEVE, 0, 36_000, start.plus(Duration.ofMinutes(30))));
        snapshots.save(new StatSnapshot(ALEX, 0, 0, start));
        snapshots.save(new StatSnapshot(ALEX, 0, 18_000, start.plus(Duration.ofMinutes(30))));

        var total = activityService.player(ALEX, 2).daily().stream().mapToDouble(d -> d.hours()).sum();

        assertThat(total).isCloseTo(0.25, within(1e-9));
    }
}
