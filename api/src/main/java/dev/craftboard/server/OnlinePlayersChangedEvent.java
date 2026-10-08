package dev.craftboard.server;

import java.util.List;

/**
 * Fired when the RCON poll sees a different player list. Empty when the server is unreachable.
 */
public record OnlinePlayersChangedEvent(List<String> onlinePlayers) {
}
