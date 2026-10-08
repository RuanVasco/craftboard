package dev.craftboard.feed;

import java.time.Instant;
import java.util.UUID;

/**
 * Something that happened on the server, taken from its log.
 *
 * @param player null for server events
 * @param detail advancement title for {@link FeedEventType#ADVANCEMENT}, null otherwise
 * @param message the log line as the game wrote it, minus the prefix
 */
public record FeedEvent(FeedEventType type, String player, UUID uuid, String detail, String message, Instant at) {
}
