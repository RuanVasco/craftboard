package dev.craftboard.feed.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import dev.craftboard.config.MinecraftProperties;
import dev.craftboard.feed.FeedEntry;
import dev.craftboard.feed.FeedEntryRepository;
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
 * Tails logs/latest.log and stores what it finds, since the server starts a fresh log on every
 * restart and every day. On startup the recent rotated logs and latest.log are read again from
 * the top, rows already stored are skipped. Events are also kept in memory to serve the feed
 * while the database is down.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    private static final int CAPACITY = 100;
    private static final int MAX_READ_BYTES = 4 << 20;
    // Log lines carry no date, so a line stamped later than now (plus some clock skew) is from yesterday.
    private static final long CLOCK_SKEW_MINUTES = 5;
    // Rotated logs are named after the day they were written, e.g. 2026-10-11-2.log.gz.
    private static final Pattern ROTATED = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})-(\\d+)\\.log\\.gz");
    private static final int BACKFILL_DAYS = 7;

    private final MinecraftProperties properties;
    private final PlayerDirectory playerDirectory;
    private final StreamService streamService;
    private final FeedEntryRepository entries;

    private final Deque<FeedEvent> events = new ArrayDeque<>();
    private final Set<String> joinedPlayers = ConcurrentHashMap.newKeySet();
    private long position;

    @Override
    public synchronized List<FeedEvent> recent() {
        try {
            return entries.findTop100ByOrderByOccurredAtDescIdDesc().stream().map(FeedEntry::toEvent).toList();
        } catch (DataAccessException e) {
            log.debug("Serving the feed from memory: {}", e.getMessage());
            return new ArrayList<>(events).reversed();
        }
    }

    @PostConstruct
    synchronized void load() {
        rotatedLogs().forEach(this::loadRotated);
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
            var lastWrite = Files.getLastModifiedTime(file).toInstant().atZone(properties.logZone());
            datedBackwards(lines, lastWrite).forEach(event -> {
                add(event);
                store(event);
            });
        } catch (IOException e) {
            log.warn("Could not read {}: {}", file, e.getMessage());
        }
    }

    private List<RotatedLog> rotatedLogs() {
        var dir = properties.latestLogFile().getParent();
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        var since = LocalDate.now(properties.logZone()).minusDays(BACKFILL_DAYS);
        try (var files = Files.list(dir)) {
            return files.flatMap(path -> RotatedLog.of(path).stream())
                    .filter(rotated -> !rotated.date().isBefore(since))
                    .sorted(Comparator.comparing(RotatedLog::date).thenComparingInt(RotatedLog::index))
                    .toList();
        } catch (IOException e) {
            log.warn("Could not list {}: {}", dir, e.getMessage());
            return List.of();
        }
    }

    private void loadRotated(RotatedLog rotated) {
        try (var in = new GZIPInputStream(Files.newInputStream(rotated.path()))) {
            var date = rotated.date();
            var previous = LocalTime.MIN;
            for (var line : new String(in.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .flatMap(line -> LogLineParser.parse(line, this::isPlayer).stream())
                    .toList()) {
                rememberJoin(line);
                if (line.time().isBefore(previous)) {
                    date = date.plusDays(1);
                }
                previous = line.time();
                var event = toEvent(line, date);
                add(event);
                store(event);
            }
        } catch (IOException e) {
            log.warn("Could not read {}: {}", rotated.path(), e.getMessage());
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
                        store(event);
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
        var now = ZonedDateTime.now(properties.logZone());
        var today = now.toLocalDate();
        var candidate = today.atTime(line.time()).atZone(properties.logZone());
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
                date.atTime(line.time()).atZone(properties.logZone()).toInstant());
    }

    private void add(FeedEvent event) {
        events.addLast(event);
        while (events.size() > CAPACITY) {
            events.removeFirst();
        }
    }

    private void store(FeedEvent event) {
        try {
            if (!entries.existsByOccurredAtAndMessage(event.at(), event.message())) {
                entries.save(new FeedEntry(event));
            }
        } catch (DataAccessException e) {
            log.warn("Could not store feed event: {}", e.getMessage());
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

    private record RotatedLog(Path path, LocalDate date, int index) {

        static Optional<RotatedLog> of(Path path) {
            var matcher = ROTATED.matcher(path.getFileName().toString());
            return matcher.matches()
                    ? Optional.of(new RotatedLog(path, LocalDate.parse(matcher.group(1)), Integer.parseInt(matcher.group(2))))
                    : Optional.empty();
        }
    }
}
