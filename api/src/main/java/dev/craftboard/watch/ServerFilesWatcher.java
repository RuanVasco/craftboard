package dev.craftboard.watch;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import dev.craftboard.config.MinecraftProperties;
import dev.craftboard.player.PlayerDirectory;
import dev.craftboard.stats.StatsFiles;
import dev.craftboard.stats.StatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServerFilesWatcher implements SmartLifecycle {

    // The server touches each file several times per save, so changes are flushed once things settle.
    private static final long DEBOUNCE_MS = 500;

    private final MinecraftProperties properties;
    private final StatsService statsService;
    private final PlayerDirectory playerDirectory;

    private WatchService watchService;
    private Thread thread;
    private volatile boolean running;

    @Override
    public void start() {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            register(properties.statsDir());
            register(properties.serverPath());
        } catch (IOException e) {
            log.error("Could not start file watcher", e);
            return;
        }
        running = true;
        thread = Thread.ofPlatform().name("server-files-watcher").daemon().start(this::loop);
    }

    @Override
    public void stop() {
        running = false;
        try {
            if (watchService != null) {
                watchService.close();
            }
        } catch (IOException e) {
            log.debug("Error closing watch service", e);
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void register(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            log.warn("Not watching {}, directory does not exist", dir.toAbsolutePath());
            return;
        }
        dir.register(watchService, ENTRY_CREATE, ENTRY_MODIFY);
        log.info("Watching {}", dir.toAbsolutePath());
    }

    private void loop() {
        Set<UUID> changedStats = new HashSet<>();
        boolean userCacheChanged = false;
        while (running) {
            try {
                WatchKey key = watchService.poll(DEBOUNCE_MS, TimeUnit.MILLISECONDS);
                if (key == null) {
                    flush(changedStats, userCacheChanged);
                    changedStats.clear();
                    userCacheChanged = false;
                    continue;
                }
                var dir = (Path) key.watchable();
                for (var event : key.pollEvents()) {
                    if (!(event.context() instanceof Path file)) {
                        continue;
                    }
                    if (dir.equals(properties.statsDir())) {
                        StatsFiles.uuidOf(file).ifPresent(changedStats::add);
                    } else if (file.equals(properties.userCacheFile().getFileName())) {
                        userCacheChanged = true;
                    }
                }
                key.reset();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (ClosedWatchServiceException e) {
                return;
            } catch (RuntimeException e) {
                log.warn("Error handling file change", e);
            }
        }
    }

    private void flush(Set<UUID> changedStats, boolean userCacheChanged) {
        if (userCacheChanged) {
            playerDirectory.reload();
            statsService.reloadAll();
            return;
        }
        changedStats.forEach(statsService::refresh);
    }
}
