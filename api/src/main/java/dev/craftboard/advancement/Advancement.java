package dev.craftboard.advancement;

import java.time.Instant;

/** {@code id} without the namespace, like {@code story/mine_diamond}. */
public record Advancement(String id, String category, Instant doneAt) {
}
