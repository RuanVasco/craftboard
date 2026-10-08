package dev.craftboard.playerdata;

/** {@code id} without the namespace, like {@code diamond_pickaxe}. */
public record ItemStack(String id, int count) {
}
