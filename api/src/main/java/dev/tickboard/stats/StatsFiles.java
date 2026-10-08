package dev.tickboard.stats;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

public final class StatsFiles {

    private static final String EXTENSION = ".json";

    private StatsFiles() {
    }

    public static Path resolve(Path statsDir, UUID uuid) {
        return statsDir.resolve(uuid + EXTENSION);
    }

    public static Optional<UUID> uuidOf(Path file) {
        var name = file.getFileName().toString();
        if (!name.endsWith(EXTENSION)) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(name.substring(0, name.length() - EXTENSION.length())));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
