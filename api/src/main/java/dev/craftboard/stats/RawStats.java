package dev.craftboard.stats;

import java.util.Map;

/**
 * Everything in a stats file, by category ({@code custom}, {@code mined}, {@code killed}, ...) and
 * then by key. The {@code minecraft:} namespace is dropped from both.
 */
public record RawStats(Map<String, Map<String, Long>> categories) {

    public static final RawStats EMPTY = new RawStats(Map.of());

    public Map<String, Long> category(String name) {
        return categories.getOrDefault(name, Map.of());
    }

    public long custom(String key) {
        return category("custom").getOrDefault(key, 0L);
    }

    public long deaths() {
        return custom("deaths");
    }

    public long playTimeTicks() {
        var custom = category("custom");
        return custom.containsKey("play_time") ? custom.get("play_time") : custom("play_one_minute");
    }

    public long mobKills() {
        return custom("mob_kills");
    }

    public long playerKills() {
        return custom("player_kills");
    }

    // Every movement stat ends in _one_cm, including the ones newer versions keep adding (mounts).
    public long distanceCm() {
        return category("custom").entrySet().stream()
                .filter(e -> e.getKey().endsWith("_one_cm"))
                .mapToLong(Map.Entry::getValue)
                .sum();
    }

    public long blocksMined() {
        return category("mined").values().stream().mapToLong(Long::longValue).sum();
    }
}
