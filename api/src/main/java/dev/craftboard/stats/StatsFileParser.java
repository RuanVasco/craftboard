package dev.craftboard.stats;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class StatsFileParser {

    private static final String NAMESPACE = "minecraft:";

    private final ObjectMapper objectMapper;

    public RawStats parse(byte[] content) {
        Map<String, Map<String, Long>> categories = new LinkedHashMap<>();
        objectMapper.readTree(content).path("stats").properties().forEach(category -> {
            Map<String, Long> values = new LinkedHashMap<>();
            category.getValue().properties().forEach(stat -> values.put(stripNamespace(stat.getKey()), stat.getValue().asLong()));
            categories.put(stripNamespace(category.getKey()), Map.copyOf(values));
        });
        return new RawStats(Map.copyOf(categories));
    }

    public static String stripNamespace(String id) {
        return id.startsWith(NAMESPACE) ? id.substring(NAMESPACE.length()) : id;
    }
}
