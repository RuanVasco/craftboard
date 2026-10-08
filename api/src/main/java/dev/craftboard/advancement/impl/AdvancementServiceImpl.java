package dev.craftboard.advancement.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import dev.craftboard.advancement.Advancement;
import dev.craftboard.advancement.AdvancementService;
import dev.craftboard.advancement.AdvancementsOverview;
import dev.craftboard.advancement.AdvancementsParser;
import dev.craftboard.advancement.PlayerAdvancement;
import dev.craftboard.config.MinecraftProperties;
import dev.craftboard.player.PlayerDirectory;
import dev.craftboard.stats.StatsFiles;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;

// Read on demand: a handful of small files, and always as fresh as the last save.
@Slf4j
@Service
@RequiredArgsConstructor
public class AdvancementServiceImpl implements AdvancementService {

    private final MinecraftProperties properties;
    private final AdvancementsParser parser;
    private final PlayerDirectory playerDirectory;

    @Override
    public List<Advancement> of(UUID uuid) {
        return read(StatsFiles.resolve(properties.advancementsDir(), uuid));
    }

    @Override
    public AdvancementsOverview overview(int recentLimit) {
        Map<UUID, Integer> counts = new HashMap<>();
        List<PlayerAdvancement> all = new ArrayList<>();
        for (var uuid : players()) {
            var name = playerDirectory.nameOf(uuid).orElse(uuid.toString());
            var advancements = of(uuid);
            counts.put(uuid, advancements.size());
            advancements.stream()
                    .filter(a -> a.doneAt() != null)
                    .forEach(a -> all.add(new PlayerAdvancement(uuid, name, a.id(), a.category(), a.doneAt())));
        }
        var recent = all.stream()
                .sorted(Comparator.comparing(PlayerAdvancement::doneAt).reversed())
                .limit(recentLimit)
                .toList();
        return new AdvancementsOverview(counts, recent);
    }

    private List<UUID> players() {
        var dir = properties.advancementsDir();
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files.map(StatsFiles::uuidOf).flatMap(Optional::stream).toList();
        } catch (IOException e) {
            log.warn("Could not list {}: {}", dir, e.getMessage());
            return List.of();
        }
    }

    private List<Advancement> read(Path file) {
        if (!Files.exists(file)) {
            return List.of();
        }
        try {
            return parser.parse(Files.readAllBytes(file));
        } catch (IOException | JacksonException e) {
            // Usually a save caught halfway, the next request reads the finished file.
            log.debug("Could not read {}: {}", file, e.getMessage());
            return List.of();
        }
    }
}
