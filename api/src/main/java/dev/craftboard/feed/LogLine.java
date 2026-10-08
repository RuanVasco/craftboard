package dev.craftboard.feed;

import java.time.LocalTime;

/** A log line the feed cares about, before it gets a date. */
public record LogLine(LocalTime time, FeedEventType type, String player, String detail, String message) {
}
