package dev.craftboard.ranking.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.craftboard.advancement.Advancement;
import dev.craftboard.advancement.AdvancementService;
import dev.craftboard.advancement.AdvancementsOverview;
import dev.craftboard.history.HistoryService;
import dev.craftboard.history.Period;
import dev.craftboard.history.SnapshotPoint;
import dev.craftboard.player.OfflineUuid;
import dev.craftboard.ranking.RankingPage;
import dev.craftboard.ranking.RankingSort;
import dev.craftboard.ranking.RankingSortConverter;
import dev.craftboard.stats.PlayerStats;

class RankingServiceImplTest {

    private static final PlayerStats STEVE = player("Steve", 300, 5);
    private static final PlayerStats ALEX = player("Alex", 500, 1);
    private static final PlayerStats NOTCH = player("Notch", 100, 9);
    private static final PlayerStats HEROBRINE = player("Herobrine", 100, 0);

    private final RankingServiceImpl service = new RankingServiceImpl(new FixedHistory(), new FixedAdvancements());

    @Test
    void ordersByTheChosenStatAndBreaksTiesByName() {
        var page = service.ranking(Period.ALL, RankingSort.PLAY_TIME, "", 0, 10);

        assertThat(page.items()).extracting(e -> e.player().name())
                .containsExactly("Alex", "Steve", "Herobrine", "Notch");
        assertThat(page.items()).extracting(RankingPage.Entry::rank).containsExactly(1, 2, 3, 4);
    }

    @Test
    void sortsByAdvancementCounts() {
        var page = service.ranking(Period.ALL, RankingSort.ADVANCEMENTS, "", 0, 10);

        assertThat(page.items()).extracting(e -> e.player().name()).startsWith("Notch", "Steve");
        assertThat(page.items().getFirst().advancements()).isEqualTo(40);
    }

    @Test
    void searchKeepsTheRealRankAndTheFullTotals() {
        var page = service.ranking(Period.ALL, RankingSort.DEATHS, "  NOT ", 0, 10);

        assertThat(page.items()).singleElement().satisfies(e -> {
            assertThat(e.player().name()).isEqualTo("Notch");
            assertThat(e.rank()).isEqualTo(1);
        });
        assertThat(page.totalItems()).isEqualTo(1);
        assertThat(page.totals().players()).isEqualTo(4);
        assertThat(page.totals().deaths()).isEqualTo(15);
        assertThat(page.maxValue()).isEqualTo(9);
    }

    @Test
    void searchMatchesUuidWithOrWithoutDashes() {
        var dashed = STEVE.uuid().toString();

        assertThat(service.ranking(Period.ALL, RankingSort.PLAY_TIME, dashed, 0, 10).items())
                .extracting(e -> e.player().name()).containsExactly("Steve");
        assertThat(service.ranking(Period.ALL, RankingSort.PLAY_TIME, dashed.replace("-", ""), 0, 10).items())
                .extracting(e -> e.player().name()).containsExactly("Steve");
    }

    @Test
    void cutsPagesAndReportsTheirCount() {
        var second = service.ranking(Period.ALL, RankingSort.PLAY_TIME, "", 1, 3);

        assertThat(second.items()).extracting(RankingPage.Entry::rank).containsExactly(4);
        assertThat(second.totalItems()).isEqualTo(4);
        assertThat(second.totalPages()).isEqualTo(2);
        assertThat(service.ranking(Period.ALL, RankingSort.PLAY_TIME, "", 9, 3).items()).isEmpty();
    }

    @Test
    void clampsPageSize() {
        assertThat(service.ranking(Period.ALL, RankingSort.PLAY_TIME, "", 0, 0).size()).isEqualTo(1);
        assertThat(service.ranking(Period.ALL, RankingSort.PLAY_TIME, "", 0, 10_000).size())
                .isEqualTo(RankingServiceImpl.MAX_PAGE_SIZE);
    }

    @Test
    void converterTakesCamelCase() {
        var converter = new RankingSortConverter();

        assertThat(converter.convert("playTime")).isEqualTo(RankingSort.PLAY_TIME);
        assertThat(converter.convert("blocksMined")).isEqualTo(RankingSort.BLOCKS_MINED);
        assertThat(converter.convert("deaths")).isEqualTo(RankingSort.DEATHS);
    }

    private static PlayerStats player(String name, long playTime, long deaths) {
        return new PlayerStats(OfflineUuid.of(name), name, deaths, playTime, 0, 0, 0, 0, Instant.EPOCH);
    }

    private static class FixedHistory implements HistoryService {

        @Override
        public List<PlayerStats> ranking(Period period) {
            return List.of(STEVE, ALEX, NOTCH, HEROBRINE);
        }

        @Override
        public List<SnapshotPoint> history(UUID uuid, Instant from, Instant to) {
            return List.of();
        }
    }

    private static class FixedAdvancements implements AdvancementService {

        @Override
        public List<Advancement> of(UUID uuid) {
            return List.of();
        }

        @Override
        public AdvancementsOverview overview(int recentLimit) {
            return new AdvancementsOverview(Map.of(NOTCH.uuid(), 40, STEVE.uuid(), 12), List.of());
        }
    }
}
