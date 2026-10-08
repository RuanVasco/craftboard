package dev.craftboard.advancement;

import java.time.Instant;
import java.util.UUID;

public record PlayerAdvancement(UUID uuid, String name, String id, String category, Instant doneAt) {
}
