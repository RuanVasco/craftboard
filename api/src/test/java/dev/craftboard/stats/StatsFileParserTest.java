package dev.craftboard.stats;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class StatsFileParserTest {

    private final StatsFileParser parser = new StatsFileParser(JsonMapper.builder().build());

    @Test
    void readsDeathsAndPlayTime() {
        var stats = parse("""
                {"stats":{"minecraft:custom":{"minecraft:deaths":3,"minecraft:play_time":72000}}}""");

        assertThat(stats).isEqualTo(new StatsFileParser.RawStats(3, 72000));
    }

    @Test
    void fallsBackToLegacyPlayTimeKey() {
        var stats = parse("""
                {"stats":{"minecraft:custom":{"minecraft:play_one_minute":100}}}""");

        assertThat(stats).isEqualTo(new StatsFileParser.RawStats(0, 100));
    }

    private StatsFileParser.RawStats parse(String json) {
        return parser.parse(json.getBytes(StandardCharsets.UTF_8));
    }
}
