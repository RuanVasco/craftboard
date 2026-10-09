package dev.craftboard.ranking;

import java.util.List;

import dev.craftboard.stats.PlayerStats;

/**
 * One page of the ranking. {@code totalItems} counts the players matching the search, while
 * {@code totals} always covers everyone in the period, so the summary does not move while searching.
 * {@code maxValue} is the leader's value for the sort, so bars on any page share one scale.
 */
public record RankingPage(List<Entry> items, int page, int size, int totalItems, int totalPages, long maxValue,
        Totals totals) {

    /** {@code rank} is the position in the full ranking, so a search result keeps its real place. */
    public record Entry(int rank, PlayerStats player, int advancements) {
    }

    public record Totals(int players, long playTimeTicks, long deaths, long mobKills, long playerKills,
            long blocksMined, long distanceCm) {

        public static Totals of(List<PlayerStats> players) {
            return new Totals(
                    players.size(),
                    players.stream().mapToLong(PlayerStats::playTimeTicks).sum(),
                    players.stream().mapToLong(PlayerStats::deaths).sum(),
                    players.stream().mapToLong(PlayerStats::mobKills).sum(),
                    players.stream().mapToLong(PlayerStats::playerKills).sum(),
                    players.stream().mapToLong(PlayerStats::blocksMined).sum(),
                    players.stream().mapToLong(PlayerStats::distanceCm).sum());
        }
    }
}
