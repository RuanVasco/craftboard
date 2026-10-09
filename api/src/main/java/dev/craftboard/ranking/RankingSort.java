package dev.craftboard.ranking;

import java.util.function.ToLongFunction;

import dev.craftboard.stats.PlayerStats;

public enum RankingSort {
    PLAY_TIME(PlayerStats::playTimeTicks),
    DEATHS(PlayerStats::deaths),
    MOB_KILLS(PlayerStats::mobKills),
    PLAYER_KILLS(PlayerStats::playerKills),
    BLOCKS_MINED(PlayerStats::blocksMined),
    DISTANCE(PlayerStats::distanceCm),
    // Advancement counts are not in the stats files, so the service resolves them on its own.
    ADVANCEMENTS(null);

    private final ToLongFunction<PlayerStats> stat;

    RankingSort(ToLongFunction<PlayerStats> stat) {
        this.stat = stat;
    }

    public ToLongFunction<PlayerStats> stat() {
        return stat;
    }
}
