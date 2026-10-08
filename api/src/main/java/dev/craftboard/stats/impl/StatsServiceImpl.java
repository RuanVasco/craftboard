package dev.craftboard.stats.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import dev.craftboard.config.MinecraftProperties;
import dev.craftboard.player.PlayerDirectory;
import dev.craftboard.stats.PlayerStats;
import dev.craftboard.stats.RawStats;
import dev.craftboard.stats.StatsFileParser;
import dev.craftboard.stats.StatsFiles;
import dev.craftboard.stats.StatsService;
import dev.craftboard.stats.StatsUpdatedEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private static final long PARTIAL_WRITE_RETRY_MS = 200;

    private final MinecraftProperties properties;
    private final PlayerDirectory playerDirectory;
    private final StatsFileParser parser;
    private final ApplicationEventPublisher events;

    private final Map<UUID, PlayerStats> cache = new ConcurrentHashMap<>();
    private final Map<UUID, RawStats> rawCache = new ConcurrentHashMap<>();

    @Override
    public List<PlayerStats> all() {
        return cache.values().stream()
                .sorted(Comparator.comparingLong(PlayerStats::playTimeTicks).reversed())
                .toList();
    }

    @Override
    public Map<UUID, RawStats> raw() {
        return Map.copyOf(rawCache);
    }

    @PostConstruct
    @Override
    public void reloadAll() {
        var dir = properties.statsDir();
        if (!Files.isDirectory(dir)) {
            log.warn("Stats directory not found at {}", dir.toAbsolutePath());
            return;
        }
        try (Stream<Path> files = Files.list(dir)) {
            files.map(StatsFiles::uuidOf)
                    .flatMap(Optional::stream)
                    .forEach(this::refresh);
        } catch (IOException e) {
            log.warn("Could not list {}: {}", dir, e.getMessage());
        }
    }

    @Override
    public void refresh(UUID uuid) {
        var file = StatsFiles.resolve(properties.statsDir(), uuid);
        if (!Files.exists(file)) {
            return;
        }
        try {
            var raw = readWithRetry(file);
            var stats = PlayerStats.of(
                    uuid,
                    playerDirectory.nameOf(uuid).orElse(uuid.toString()),
                    raw,
                    Files.getLastModifiedTime(file).toInstant());
            var previousRaw = rawCache.put(uuid, raw);
            var previous = cache.put(uuid, stats);
            // Any stat counts as a change, so open profiles refresh even when the summary did not move.
            if (previous == null || !raw.equals(previousRaw) || !previous.name().equals(stats.name())) {
                events.publishEvent(new StatsUpdatedEvent(stats));
            }
        } catch (IOException | JacksonException e) {
            log.warn("Could not read stats for {}: {}", uuid, e.getMessage());
        }
    }

    // The server may still be writing the file when the watcher fires.
    private RawStats readWithRetry(Path file) throws IOException {
        try {
            return parser.parse(Files.readAllBytes(file));
        } catch (JacksonException e) {
            try {
                Thread.sleep(PARTIAL_WRITE_RETRY_MS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw e;
            }
            return parser.parse(Files.readAllBytes(file));
        }
    }
}
