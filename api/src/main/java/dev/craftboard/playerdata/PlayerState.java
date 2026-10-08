package dev.craftboard.playerdata;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * What the player data file says about a player at the last save. Position is left out on
 * purpose: the dashboard is public and coordinates would point straight at everyone's base.
 *
 * @param equipment worn armor and offhand, keyed by {@code head}, {@code chest}, {@code legs},
 *                  {@code feet} and {@code offhand}
 * @param inventory main inventory with stacks of the same item added up, largest first
 */
public record PlayerState(
        int xpLevel,
        float health,
        int foodLevel,
        String dimension,
        String gameMode,
        ItemStack selectedItem,
        Map<String, ItemStack> equipment,
        List<ItemStack> inventory,
        Instant updatedAt) {
}
