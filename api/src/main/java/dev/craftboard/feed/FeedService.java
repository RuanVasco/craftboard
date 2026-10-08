package dev.craftboard.feed;

import java.util.List;

public interface FeedService {

    /** Latest events, newest first. */
    List<FeedEvent> recent();
}
