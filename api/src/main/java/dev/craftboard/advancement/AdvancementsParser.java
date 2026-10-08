package dev.craftboard.advancement;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import dev.craftboard.stats.StatsFileParser;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class AdvancementsParser {

    // "2024-05-01 18:22:31 -0300"
    private static final DateTimeFormatter CRITERION_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z");

    private final ObjectMapper objectMapper;

    /** Completed advancements only, oldest first. Recipe unlocks share the file but are not achievements. */
    public List<Advancement> parse(byte[] content) {
        List<Advancement> done = new ArrayList<>();
        objectMapper.readTree(content).properties().forEach(entry -> {
            var id = StatsFileParser.stripNamespace(entry.getKey());
            var node = entry.getValue();
            if (!node.isObject() || id.startsWith("recipes/") || !node.path("done").asBoolean()) {
                return;
            }
            int slash = id.indexOf('/');
            done.add(new Advancement(id, slash > 0 ? id.substring(0, slash) : id, completedAt(node.path("criteria"))));
        });
        done.sort(Comparator.comparing(Advancement::doneAt, Comparator.nullsFirst(Comparator.naturalOrder())));
        return done;
    }

    // An advancement is done when its last criterion is met.
    private static Instant completedAt(JsonNode criteria) {
        return criteria.valueStream()
                .map(value -> parseTime(value.asString()))
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    private static Instant parseTime(String value) {
        try {
            return OffsetDateTime.parse(value, CRITERION_TIME).toInstant();
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
