package dev.craftboard.playerdata;

import static dev.craftboard.nbt.NbtPath.compound;
import static dev.craftboard.nbt.NbtPath.list;
import static dev.craftboard.nbt.NbtPath.number;
import static dev.craftboard.nbt.NbtPath.string;

import java.io.IOException;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import dev.craftboard.nbt.Nbt;
import dev.craftboard.stats.StatsFileParser;

@Component
public class PlayerDataParser {

    private static final List<String> GAME_MODES = List.of("survival", "creative", "adventure", "spectator");
    private static final List<String> EQUIPMENT_SLOTS = List.of("head", "chest", "legs", "feet", "offhand");
    // Where armor and offhand lived inside Inventory before they moved to their own compound.
    private static final Map<Integer, String> LEGACY_EQUIPMENT_SLOTS =
            Map.of(103, "head", 102, "chest", 101, "legs", 100, "feet", -106, "offhand");
    private static final int MAIN_INVENTORY_SIZE = 36;

    public PlayerState parse(byte[] content, Instant updatedAt) throws IOException {
        var root = Nbt.read(content);
        var inventory = list(root, "Inventory").orElse(List.of()).stream()
                .flatMap(PlayerDataParser::asCompound)
                .toList();

        Map<String, ItemStack> equipment = new LinkedHashMap<>();
        var modern = compound(root, "equipment").orElse(Map.of());
        for (var slot : EQUIPMENT_SLOTS) {
            compound(modern, slot).flatMap(PlayerDataParser::item).ifPresent(item -> equipment.put(slot, item));
        }
        Map<Integer, Map<String, Object>> bySlot = new LinkedHashMap<>();
        inventory.forEach(entry -> number(entry, "Slot").ifPresent(slot -> bySlot.put(slot.intValue(), entry)));
        LEGACY_EQUIPMENT_SLOTS.forEach((index, slot) -> Optional.ofNullable(bySlot.get(index))
                .flatMap(PlayerDataParser::item)
                .ifPresent(item -> equipment.putIfAbsent(slot, item)));

        var selected = number(root, "SelectedItemSlot")
                .map(slot -> bySlot.get(slot.intValue()))
                .flatMap(PlayerDataParser::item)
                .orElse(null);

        Map<String, Integer> totals = new LinkedHashMap<>();
        bySlot.forEach((slot, entry) -> {
            if (slot >= 0 && slot < MAIN_INVENTORY_SIZE) {
                item(entry).ifPresent(item -> totals.merge(item.id(), item.count(), Integer::sum));
            }
        });
        var items = totals.entrySet().stream()
                .map(e -> new ItemStack(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingInt(ItemStack::count).reversed().thenComparing(ItemStack::id))
                .toList();

        int gameMode = number(root, "playerGameType").map(Number::intValue).orElse(0);
        return new PlayerState(
                number(root, "XpLevel").map(Number::intValue).orElse(0),
                number(root, "Health").map(Number::floatValue).orElse(0f),
                number(root, "foodLevel").map(Number::intValue).orElse(0),
                string(root, "Dimension").map(StatsFileParser::stripNamespace).orElse("overworld"),
                gameMode >= 0 && gameMode < GAME_MODES.size() ? GAME_MODES.get(gameMode) : GAME_MODES.getFirst(),
                selected,
                equipment,
                items,
                updatedAt);
    }

    @SuppressWarnings("unchecked")
    private static Stream<Map<String, Object>> asCompound(Object value) {
        return value instanceof Map<?, ?> map ? Stream.of((Map<String, Object>) map) : Stream.empty();
    }

    // Item stacks spell the count "count" (int) since 1.20.5 and "Count" (byte) before.
    private static Optional<ItemStack> item(Map<String, Object> entry) {
        return string(entry, "id")
                .filter(id -> !id.equals("minecraft:air"))
                .map(id -> new ItemStack(StatsFileParser.stripNamespace(id),
                        number(entry, "count").or(() -> number(entry, "Count")).map(Number::intValue).orElse(1)));
    }
}
