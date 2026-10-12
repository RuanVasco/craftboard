package dev.craftboard.feed.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.craftboard.IntegrationTest;
import dev.craftboard.config.MinecraftProperties;
import dev.craftboard.feed.FeedEntryRepository;
import dev.craftboard.feed.FeedEvent;
import dev.craftboard.feed.FeedEventType;
import dev.craftboard.player.PlayerDirectory;
import dev.craftboard.stream.StreamService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class FeedServiceImplTest extends IntegrationTest {

    private final FeedServiceImpl feedService;
    private final FeedEntryRepository entries;
    private final PlayerDirectory playerDirectory;
    private final StreamService streamService;

    @TempDir
    Path serverDir;

    @Test
    void storesTheLogOnceEvenWhenReadAgain() {
        feedService.load();
        long stored = entries.count();

        feedService.load();

        assertThat(stored).isPositive();
        assertThat(entries.count()).isEqualTo(stored);
        assertThat(feedService.recent()).hasSize((int) stored)
                .last().extracting(FeedEvent::type).isEqualTo(FeedEventType.SERVER_START);
    }

    @Test
    void backfillsRotatedLogsDatedByTheirName() throws IOException {
        var day = LocalDate.now(ZoneOffset.UTC).minusDays(1);
        var logs = Files.createDirectories(serverDir.resolve("logs"));
        gzip(logs.resolve(day + "-1.log.gz"), """
                [23:30:00] [Server thread/INFO]: System chat: Steve joined the game
                [00:10:00] [Server thread/INFO]: System chat: Steve left the game
                """);
        var properties = new MinecraftProperties(serverDir.toString(), "world", "localhost",
                ZoneId.of("America/Sao_Paulo"), ZoneOffset.UTC, null);

        new FeedServiceImpl(properties, playerDirectory, streamService, entries).load();

        assertThat(entries.findTop100ByOrderByOccurredAtDescIdDesc())
                .extracting(entry -> entry.toEvent().at())
                .containsExactly(
                        day.plusDays(1).atTime(0, 10).toInstant(ZoneOffset.UTC),
                        day.atTime(23, 30).toInstant(ZoneOffset.UTC));
    }

    private static void gzip(Path file, String content) throws IOException {
        try (var out = new GZIPOutputStream(Files.newOutputStream(file))) {
            out.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }
}
