package dev.craftboard.server;

import java.util.List;

/**
 * Fired after every RCON poll, changed or not, so listeners can recover from a failed update.
 * Empty when the server is unreachable.
 */
public record OnlinePlayersPolledEvent(List<String> onlinePlayers) {
}
