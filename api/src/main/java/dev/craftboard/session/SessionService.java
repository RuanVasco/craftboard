package dev.craftboard.session;

import java.util.List;

public interface SessionService {

    /**
     * Opens a session for every online player without one and closes the sessions of everyone
     * else. Called on every poll with the full list, so a failed call or a restart heals
     * on the next one.
     */
    void reconcile(List<String> onlinePlayers);
}
