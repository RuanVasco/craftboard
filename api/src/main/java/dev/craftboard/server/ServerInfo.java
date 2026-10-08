package dev.craftboard.server;

import java.util.List;

/**
 * {@code online} and {@code onlinePlayers} are null when RCON is disabled, since the files
 * alone cannot tell who is connected.
 */
public record ServerInfo(
        String address,
        String motd,
        int maxPlayers,
        boolean rconEnabled,
        Boolean online,
        List<String> onlinePlayers) {
}
