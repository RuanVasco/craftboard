package dev.craftboard.advancement;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class AdvancementsParserTest {

    private final AdvancementsParser parser = new AdvancementsParser(JsonMapper.builder().build());

    @Test
    void keepsOnlyCompletedAdvancementsOldestFirst() {
        var advancements = parse("""
                {
                  "minecraft:recipes/misc/crafting_table": {"criteria": {"has_planks": "2024-05-01 10:00:00 -0300"}, "done": true},
                  "minecraft:story/mine_diamond": {"criteria": {"diamond": "2024-05-03 12:00:00 -0300"}, "done": true},
                  "minecraft:story/mine_stone": {"criteria": {"get_stone": "2024-05-01 18:22:31 -0300"}, "done": true},
                  "minecraft:adventure/adventuring_time": {"criteria": {"minecraft:plains": "2024-05-01 10:00:00 -0300"}, "done": false},
                  "DataVersion": 4600
                }""");

        assertThat(advancements).containsExactly(
                new Advancement("story/mine_stone", "story", Instant.parse("2024-05-01T21:22:31Z")),
                new Advancement("story/mine_diamond", "story", Instant.parse("2024-05-03T15:00:00Z")));
    }

    @Test
    void completionTimeIsTheLastCriterion() {
        var advancements = parse("""
                {"minecraft:husbandry/balanced_diet": {"criteria": {
                  "apple": "2024-05-01 10:00:00 +0000", "bread": "2024-06-01 10:00:00 +0000"}, "done": true}}""");

        assertThat(advancements).singleElement()
                .extracting(Advancement::doneAt)
                .isEqualTo(Instant.parse("2024-06-01T10:00:00Z"));
    }

    private List<Advancement> parse(String json) {
        return parser.parse(json.getBytes(StandardCharsets.UTF_8));
    }
}
