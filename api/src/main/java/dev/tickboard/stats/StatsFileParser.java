package dev.tickboard.stats;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class StatsFileParser {

    private final ObjectMapper objectMapper;

    public RawStats parse(byte[] content) {
        JsonNode custom = objectMapper.readTree(content).path("stats").path("minecraft:custom");
        long playTime = custom.has("minecraft:play_time")
                ? custom.path("minecraft:play_time").asLong()
                : custom.path("minecraft:play_one_minute").asLong();
        return new RawStats(custom.path("minecraft:deaths").asLong(), playTime);
    }

    public record RawStats(long deaths, long playTimeTicks) {
    }
}
