package dev.craftboard.overview;

import java.util.List;
import java.util.UUID;

/**
 * Server wide highlights built from every stats file.
 *
 * @param records    who leads each tracked stat, one entry per stat anyone has scored in
 * @param topMined   blocks mined by everyone, most first
 * @param topKilled  mobs killed by everyone, most first
 * @param topKilledBy what killed players the most
 */
public record Overview(List<StatRecord> records, List<Count> topMined, List<Count> topKilled, List<Count> topKilledBy) {

    public record StatRecord(String key, UUID uuid, String name, long value) {
    }

    public record Count(String id, long value) {
    }
}
