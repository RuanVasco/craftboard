package dev.craftboard.advancement;

import java.util.List;
import java.util.UUID;

public interface AdvancementService {

    List<Advancement> of(UUID uuid);

    /** How many advancements each player has, plus the latest ones across the server. */
    AdvancementsOverview overview(int recentLimit);
}
