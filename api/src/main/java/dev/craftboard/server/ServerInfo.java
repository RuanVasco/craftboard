package dev.craftboard.server;

import java.util.List;

import dev.craftboard.world.WorldInfo;

/**
 * {@code online} and {@code onlinePlayers} are null when RCON is disabled, since the files
 * alone cannot tell who is connected. {@code world} is null when level.dat cannot be read.
 */
public record ServerInfo(
        String address,
        String motd,
        int maxPlayers,
        String difficulty,
        String gameMode,
        boolean hardcore,
        boolean pvp,
        boolean rconEnabled,
        Boolean online,
        List<String> onlinePlayers,
        WorldInfo world) {
}
