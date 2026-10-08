package dev.craftboard.stats;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class StatsFileParserTest {

    private final StatsFileParser parser = new StatsFileParser(JsonMapper.builder().build());

    @Test
    void readsDeathsAndPlayTime() {
        var stats = parse("""
                {"stats":{"minecraft:custom":{"minecraft:deaths":3,"minecraft:play_time":72000}}}""");

        assertThat(stats.deaths()).isEqualTo(3);
        assertThat(stats.playTimeTicks()).isEqualTo(72000);
    }

    @Test
    void fallsBackToLegacyPlayTimeKey() {
        var stats = parse("""
                {"stats":{"minecraft:custom":{"minecraft:play_one_minute":100}}}""");

        assertThat(stats.deaths()).isZero();
        assertThat(stats.playTimeTicks()).isEqualTo(100);
    }

    @Test
    void keepsEveryCategoryWithoutTheNamespace() {
        var stats = parse("""
                {"stats":{
                  "minecraft:custom":{"minecraft:mob_kills":7,"minecraft:player_kills":1,
                    "minecraft:walk_one_cm":1000,"minecraft:boat_one_cm":500,"minecraft:jump":9},
                  "minecraft:mined":{"minecraft:stone":40,"minecraft:diamond_ore":2},
                  "minecraft:killed_by":{"minecraft:creeper":3}}}""");

        assertThat(stats.mobKills()).isEqualTo(7);
        assertThat(stats.playerKills()).isEqualTo(1);
        assertThat(stats.distanceCm()).isEqualTo(1500);
        assertThat(stats.blocksMined()).isEqualTo(42);
        assertThat(stats.custom("jump")).isEqualTo(9);
        assertThat(stats.category("killed_by")).isEqualTo(Map.of("creeper", 3L));
    }

    @Test
    void missingStatsReadAsZero() {
        var stats = parse("""
                {"DataVersion":4600}""");

        assertThat(stats).isEqualTo(RawStats.EMPTY);
        assertThat(stats.distanceCm()).isZero();
    }

    private RawStats parse(String json) {
        return parser.parse(json.getBytes(StandardCharsets.UTF_8));
    }
}
