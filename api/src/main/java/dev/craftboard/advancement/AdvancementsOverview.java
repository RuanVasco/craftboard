package dev.craftboard.advancement;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AdvancementsOverview(Map<UUID, Integer> counts, List<PlayerAdvancement> recent) {
}
