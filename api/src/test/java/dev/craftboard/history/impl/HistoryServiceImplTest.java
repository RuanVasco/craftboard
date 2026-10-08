package dev.craftboard.history.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import dev.craftboard.IntegrationTest;
import dev.craftboard.history.Period;
import dev.craftboard.history.StatSnapshot;
import dev.craftboard.history.StatSnapshotRepository;
import dev.craftboard.player.OfflineUuid;
import dev.craftboard.player.Player;
import dev.craftboard.player.PlayerRepository;
import dev.craftboard.stats.PlayerStats;
import dev.craftboard.stats.StatsService;
import dev.craftboard.stats.StatsUpdatedEvent;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class HistoryServiceImplTest extends IntegrationTest {

    private static final UUID STEVE = OfflineUuid.of("Steve");

    private final HistoryServiceImpl historyService;
    private final StatsService statsService;
    private final PlayerRepository players;
    private final StatSnapshotRepository snapshots;
    private final MockMvc mvc;

    @Test
    void backfillOnEmptyDatabaseStoresCurrentTotalsOnly() {
        historyService.backfill();

        assertThat(snapshots.count()).isEqualTo(statsService.all().size());
        assertThat(snapshots.findAll()).noneMatch(s -> s.getPlayTimeTicks() == 0 && s.getDeaths() == 0);
    }

    @Test
    void newPlayerAfterBackfillStartsFromZero() {
        historyService.backfill();
        var newcomer = new PlayerStats(UUID.randomUUID(), "Newcomer", 1, 500, Instant.now());

        historyService.onStatsUpdated(new StatsUpdatedEvent(newcomer));

        assertThat(historyService.history(newcomer.uuid(), null, null))
                .extracting(p -> p.playTimeTicks())
                .containsExactly(0L, 500L);
    }

    @Test
    void unchangedValuesDoNotCreateSnapshots() {
        historyService.backfill();
        long before = snapshots.count();

        historyService.backfill();
        historyService.onStatsUpdated(new StatsUpdatedEvent(steve()));

        assertThat(snapshots.count()).isEqualTo(before);
    }

    @Test
    void periodRankingCountsOnlyWhatWasGainedSinceTheBaseline() {
        var steve = steve();
        var tenDaysAgo = Instant.now().minus(Duration.ofDays(10));
        players.save(new Player(STEVE, "Steve", tenDaysAgo));
        snapshots.save(new StatSnapshot(STEVE, steve.deaths() - 2, steve.playTimeTicks() - 1000, tenDaysAgo));

        var week = historyService.ranking(Period.WEEK);

        assertThat(week).singleElement().satisfies(gained -> {
            assertThat(gained.uuid()).isEqualTo(STEVE);
            assertThat(gained.deaths()).isEqualTo(2);
            assertThat(gained.playTimeTicks()).isEqualTo(1000);
        });
        assertThat(historyService.ranking(Period.ALL)).isEqualTo(statsService.all());
    }

    @Test
    void playersEndpointAcceptsLowercasePeriod() throws Exception {
        mvc.perform(get("/api/players").param("period", "week")).andExpect(status().isOk());
        mvc.perform(get("/api/players").param("period", "year")).andExpect(status().isBadRequest());
    }

    @Test
    void historyEndpointReturnsTheSeries() throws Exception {
        historyService.backfill();

        mvc.perform(get("/api/players/{uuid}/history", STEVE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].playTimeTicks").value(steve().playTimeTicks()));
    }

    private PlayerStats steve() {
        return statsService.all().stream().filter(p -> p.uuid().equals(STEVE)).findFirst().orElseThrow();
    }
}
