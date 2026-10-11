package dev.craftboard.overview.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.craftboard.overview.Overview;
import dev.craftboard.player.OfflineUuid;
import dev.craftboard.player.PlayerDirectory;
import dev.craftboard.session.SessionLength;
import dev.craftboard.session.SessionService;
import dev.craftboard.stats.PlayerStats;
import dev.craftboard.stats.RawStats;
import dev.craftboard.stats.StatsService;

class OverviewServiceImplTest {

    private static final UUID STEVE = OfflineUuid.of("Steve");
    private static final UUID ALEX = OfflineUuid.of("Alex");

    private final Map<UUID, RawStats> raw = Map.of(
            STEVE, new RawStats(Map.of(
                    "custom", Map.of("jump", 10L, "walk_one_cm", 500L),
                    "mined", Map.of("stone", 100L, "diamond_ore", 2L),
                    "killed_by", Map.of("creeper", 3L))),
            ALEX, new RawStats(Map.of(
                    "custom", Map.of("jump", 30L),
                    "mined", Map.of("stone", 50L, "deepslate_diamond_ore", 5L),
                    "killed_by", Map.of("creeper", 1L, "zombie", 2L))));

    private final OverviewServiceImpl service = new OverviewServiceImpl(new FixedStats(), new Names(), new FixedSessions());

    @Test
    void picksTheLeaderOfEachStatAndSkipsStatsNobodyScored() {
        var records = service.overview().records();

        assertThat(records).contains(
                new Overview.StatRecord("jump", ALEX, "Alex", 30),
                new Overview.StatRecord("distance", STEVE, "Steve", 500),
                new Overview.StatRecord("diamonds", ALEX, "Alex", 5));
        assertThat(records).extracting(Overview.StatRecord::key).doesNotContain("fish_caught");
    }

    @Test
    void includesTheLongestSession() {
        assertThat(service.overview().records())
                .contains(new Overview.StatRecord("longest_session", STEVE, "Steve", 7200));
    }

    @Test
    void addsUpCategoriesAcrossPlayers() {
        var overview = service.overview();

        assertThat(overview.topMined()).first().isEqualTo(new Overview.Count("stone", 150));
        assertThat(overview.topKilledBy()).containsExactly(
                new Overview.Count("creeper", 4), new Overview.Count("zombie", 2));
    }

    private class FixedStats implements StatsService {

        @Override
        public List<PlayerStats> all() {
            return List.of();
        }

        @Override
        public Map<UUID, RawStats> raw() {
            return raw;
        }

        @Override
        public void reloadAll() {
        }

        @Override
        public void refresh(UUID uuid) {
        }
    }

    private static class Names implements PlayerDirectory {

        @Override
        public Optional<String> nameOf(UUID uuid) {
            return Optional.of(uuid.equals(STEVE) ? "Steve" : "Alex");
        }

        @Override
        public void reload() {
        }
    }

    private static class FixedSessions implements SessionService {

        @Override
        public void reconcile(List<String> onlinePlayers) {
        }

        @Override
        public Optional<SessionLength> longest() {
            return Optional.of(new SessionLength(STEVE, 7200));
        }
    }
}
