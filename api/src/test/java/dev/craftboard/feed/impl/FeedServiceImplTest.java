package dev.craftboard.feed.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.craftboard.IntegrationTest;
import dev.craftboard.feed.FeedEntryRepository;
import dev.craftboard.feed.FeedEventType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class FeedServiceImplTest extends IntegrationTest {

    private final FeedServiceImpl feedService;
    private final FeedEntryRepository entries;

    @Test
    void storesTheLogOnceEvenWhenReadAgain() {
        feedService.load();
        long stored = entries.count();

        feedService.load();

        assertThat(stored).isPositive();
        assertThat(entries.count()).isEqualTo(stored);
        assertThat(feedService.recent()).hasSize((int) stored)
                .last().extracting("type").isEqualTo(FeedEventType.SERVER_START);
    }
}
