package dev.craftboard.session;

import java.util.List;
import java.util.Optional;

public interface SessionService {

    /**
     * Opens a session for every online player without one and closes the sessions of everyone
     * else. Called on every poll with the full list, so a failed call or a restart heals
     * on the next one.
     */
    void reconcile(List<String> onlinePlayers);

    /** The longest session ever recorded, empty without sessions or when the database is down. */
    Optional<SessionLength> longest();
}
