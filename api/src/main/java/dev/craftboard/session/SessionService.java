package dev.craftboard.session;

import java.util.List;

public interface SessionService {

    /**
     * Opens a session for every online player without one and closes the sessions of everyone
     * else. Works from the full list, so a missed poll or a restart heals on the next call.
     */
    void reconcile(List<String> onlinePlayers);
}
