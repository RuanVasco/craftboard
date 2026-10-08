package dev.craftboard.overview.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToLongFunction;

import org.springframework.stereotype.Service;

import dev.craftboard.overview.Overview;
import dev.craftboard.overview.OverviewService;
import dev.craftboard.player.PlayerDirectory;
import dev.craftboard.stats.RawStats;
import dev.craftboard.stats.StatsService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OverviewServiceImpl implements OverviewService {

    private static final int TOP_SIZE = 10;
    private static final List<String> CUSTOM_RECORDS = List.of(
            "time_since_death", "mob_kills", "player_kills", "jump", "damage_dealt", "damage_taken",
            "fish_caught", "animals_bred", "traded_with_villager", "enchant_item", "sleep_in_bed",
            "aviate_one_cm", "boat_one_cm", "horse_one_cm", "swim_one_cm", "open_chest", "leave_game");

    private final StatsService statsService;
    private final PlayerDirectory playerDirectory;

    @Override
    public Overview overview() {
        var raw = statsService.raw();

        Map<String, ToLongFunction<RawStats>> records = new LinkedHashMap<>();
        records.put("distance", RawStats::distanceCm);
        records.put("blocks_mined", RawStats::blocksMined);
        records.put("diamonds", s -> s.category("mined").getOrDefault("diamond_ore", 0L)
                + s.category("mined").getOrDefault("deepslate_diamond_ore", 0L));
        records.put("ancient_debris", s -> s.category("mined").getOrDefault("ancient_debris", 0L));
        records.put("deaths", RawStats::deaths);
        CUSTOM_RECORDS.forEach(key -> records.put(key, s -> s.custom(key)));

        List<Overview.StatRecord> leaders = new ArrayList<>();
        records.forEach((key, value) -> raw.entrySet().stream()
                .filter(e -> value.applyAsLong(e.getValue()) > 0)
                .max(Comparator.comparingLong((Map.Entry<UUID, RawStats> e) -> value.applyAsLong(e.getValue())))
                .ifPresent(e -> leaders.add(new Overview.StatRecord(key, e.getKey(), nameOf(e.getKey()),
                        value.applyAsLong(e.getValue())))));

        return new Overview(leaders, top(raw, "mined"), top(raw, "killed"), top(raw, "killed_by"));
    }

    private String nameOf(UUID uuid) {
        return playerDirectory.nameOf(uuid).orElse(uuid.toString());
    }

    private static List<Overview.Count> top(Map<UUID, RawStats> raw, String category) {
        Map<String, Long> totals = new HashMap<>();
        raw.values().forEach(stats -> stats.category(category).forEach((id, value) -> totals.merge(id, value, Long::sum)));
        return totals.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(TOP_SIZE)
                .map(e -> new Overview.Count(e.getKey(), e.getValue()))
                .toList();
    }
}
