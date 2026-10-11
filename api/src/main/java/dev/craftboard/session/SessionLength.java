package dev.craftboard.session;

import java.util.UUID;

/** How long a session lasted, counting an open one up to now. */
public record SessionLength(UUID playerUuid, long seconds) {
}
