package dev.craftboard.feed.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import dev.craftboard.config.MinecraftProperties;
import dev.craftboard.feed.FeedEvent;
import dev.craftboard.feed.FeedService;
import dev.craftboard.feed.LogLine;
import dev.craftboard.feed.LogLineParser;
import dev.craftboard.player.OfflineUuid;
import dev.craftboard.player.PlayerDirectory;
import dev.craftboard.stream.StreamService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tails logs/latest.log. Events only live in memory: the server starts a fresh log on every
 * restart, so the feed shows what happened since then.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    private static final int CAPACITY = 100;
    private static final int MAX_READ_BYTES = 4 << 20;
    // Log lines carry no date, so a line stamped later than now (plus some clock skew) is from yesterday.
    private static final long CLOCK_SKEW_MINUTES = 5;

    private final MinecraftProperties properties;
    private final PlayerDirectory playerDirectory;
    private final StreamService streamService;

    private final Deque<FeedEvent> events = new ArrayDeque<>();
    private final Set<String> joinedPlayers = ConcurrentHashMap.newKeySet();
    private long position;

    @Override
    public synchronized List<FeedEvent> recent() {
        return new ArrayList<>(events).reversed();
    }

    @PostConstruct
    synchronized void load() {
        var file = properties.latestLogFile();
        if (!Files.exists(file)) {
            log.info("No server log at {}, the feed stays empty", file.toAbsolutePath());
            return;
        }
        try {
            var bytes = Files.readAllBytes(file);
            int end = lastNewline(bytes, bytes.length) + 1;
            position = end;
            List<LogLine> lines = new ArrayList<>();
            new String(bytes, 0, end, StandardCharsets.UTF_8).lines()
                    .flatMap(line -> LogLineParser.parse(line, this::isPlayer).stream())
                    .forEach(line -> {
                        rememberJoin(line);
                        lines.add(line);
                    });
            var lastWrite = Files.getLastModifiedTime(file).toInstant().atZone(properties.timezone());
            datedBackwards(lines, lastWrite).forEach(this::add);
        } catch (IOException e) {
            log.warn("Could not read {}: {}", file, e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 2000, initialDelay = 2000)
    public synchronized void poll() {
        var file = properties.latestLogFile();
        if (!Files.exists(file)) {
            return;
        }
        try (var channel = FileChannel.open(file, StandardOpenOption.READ)) {
            long size = channel.size();
            if (size < position) {
                // A restart rotated the log and started a fresh one.
                position = 0;
            }
            if (size == position) {
                return;
            }
            var buffer = ByteBuffer.allocate((int) Math.min(size - position, MAX_READ_BYTES));
            channel.read(buffer, position);
            var bytes = buffer.array();
            // A line still being written stays for the next poll.
            int end = lastNewline(bytes, buffer.position()) + 1;
            if (end == 0) {
                return;
            }
            position += end;
            new String(bytes, 0, end, StandardCharsets.UTF_8).lines()
                    .flatMap(line -> LogLineParser.parse(line, this::isPlayer).stream())
                    .forEach(line -> {
                        rememberJoin(line);
                        var event = toEvent(line, liveDate(line));
                        add(event);
                        streamService.broadcast("feed", event);
                    });
        } catch (IOException e) {
            log.debug("Could not tail {}: {}", file, e.getMessage());
        }
    }

    private boolean isPlayer(String name) {
        return joinedPlayers.contains(name) || playerDirectory.nameOf(OfflineUuid.of(name)).isPresent();
    }

    private void rememberJoin(LogLine line) {
        if (line.player() != null) {
            joinedPlayers.add(line.player());
        }
    }

    private LocalDate liveDate(LogLine line) {
        var now = ZonedDateTime.now(properties.timezone());
        var today = now.toLocalDate();
        var candidate = today.atTime(line.time()).atZone(properties.timezone());
        return candidate.isAfter(now.plusMinutes(CLOCK_SKEW_MINUTES)) ? today.minusDays(1) : today;
    }

    // Walks back from the last write to the file, stepping a day back whenever the clock jumps forward.
    private List<FeedEvent> datedBackwards(List<LogLine> lines, ZonedDateTime lastWrite) {
        List<FeedEvent> dated = new ArrayList<>(lines.size());
        var date = lastWrite.toLocalDate();
        var previous = lastWrite.toLocalDateTime().plusMinutes(CLOCK_SKEW_MINUTES);
        for (var line : lines.reversed()) {
            var at = date.atTime(line.time());
            if (at.isAfter(previous)) {
                date = date.minusDays(1);
                at = date.atTime(line.time());
            }
            previous = at;
            dated.add(toEvent(line, date));
        }
        return dated.reversed();
    }

    private FeedEvent toEvent(LogLine line, LocalDate date) {
        return new FeedEvent(
                line.type(),
                line.player(),
                line.player() == null ? null : OfflineUuid.of(line.player()),
                line.detail(),
                line.message(),
                date.atTime(line.time()).atZone(properties.timezone()).toInstant());
    }

    private void add(FeedEvent event) {
        events.addLast(event);
        while (events.size() > CAPACITY) {
            events.removeFirst();
        }
    }

    private static int lastNewline(byte[] bytes, int length) {
        for (int i = length - 1; i >= 0; i--) {
            if (bytes[i] == '\n') {
                return i;
            }
        }
        return -1;
    }
}
