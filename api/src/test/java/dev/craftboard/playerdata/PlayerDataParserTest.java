package dev.craftboard.playerdata;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import dev.craftboard.nbt.NbtWriter;

class PlayerDataParserTest {

    private final PlayerDataParser parser = new PlayerDataParser();

    @Test
    void readsStatusEquipmentAndInventory() throws IOException {
        var state = parse(Map.of(
                "XpLevel", 31,
                "Health", 18.0f,
                "foodLevel", 17,
                "Dimension", "minecraft:the_nether",
                "playerGameType", 0,
                "SelectedItemSlot", 1,
                "Pos", List.of(100.0, 64.0, -200.0),
                "Inventory", List.of(
                        Map.of("Slot", (byte) 0, "id", "minecraft:cobblestone", "count", 64),
                        Map.of("Slot", (byte) 1, "id", "minecraft:diamond_pickaxe", "count", 1),
                        Map.of("Slot", (byte) 2, "id", "minecraft:cobblestone", "count", 30)),
                "equipment", Map.of(
                        "head", Map.of("id", "minecraft:netherite_helmet", "count", 1),
                        "offhand", Map.of("id", "minecraft:shield", "count", 1))));

        assertThat(state.xpLevel()).isEqualTo(31);
        assertThat(state.health()).isEqualTo(18.0f);
        assertThat(state.foodLevel()).isEqualTo(17);
        assertThat(state.dimension()).isEqualTo("the_nether");
        assertThat(state.gameMode()).isEqualTo("survival");
        assertThat(state.selectedItem()).isEqualTo(new ItemStack("diamond_pickaxe", 1));
        assertThat(state.equipment()).containsExactly(
                Map.entry("head", new ItemStack("netherite_helmet", 1)),
                Map.entry("offhand", new ItemStack("shield", 1)));
        assertThat(state.inventory()).containsExactly(
                new ItemStack("cobblestone", 94), new ItemStack("diamond_pickaxe", 1));
    }

    @Test
    void readsArmorFromLegacyInventorySlots() throws IOException {
        var state = parse(Map.of("Inventory", List.of(
                Map.of("Slot", (byte) 103, "id", "minecraft:iron_helmet", "Count", (byte) 1),
                Map.of("Slot", (byte) 100, "id", "minecraft:iron_boots", "Count", (byte) 1))));

        assertThat(state.equipment()).containsOnly(
                Map.entry("head", new ItemStack("iron_helmet", 1)),
                Map.entry("feet", new ItemStack("iron_boots", 1)));
        assertThat(state.inventory()).isEmpty();
    }

    private PlayerState parse(Map<String, Object> root) throws IOException {
        return parser.parse(NbtWriter.gzipped(root), Instant.EPOCH);
    }
}
