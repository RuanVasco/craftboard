package dev.craftboard.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("craftboard")
public record MinecraftProperties(
        @DefaultValue("../dev-data") String serverDir,
        @DefaultValue("world") String levelName,
        @DefaultValue("localhost") String publicAddress,
        // Zone activity is bucketed by, and the one the server logs in unless logTimezone says otherwise.
        @DefaultValue("America/Sao_Paulo") ZoneId timezone,
        // Docker images often run the server in UTC while players live elsewhere.
        ZoneId logTimezone,
        @DefaultValue Rcon rcon) {

    public ZoneId logZone() {
        return logTimezone != null ? logTimezone : timezone;
    }

    public Path serverPath() {
        return Path.of(serverDir);
    }

    public Path worldPath() {
        return serverPath().resolve(levelName);
    }

    public Path statsDir() {
        return playerFolder("stats", "stats");
    }

    public Path advancementsDir() {
        return playerFolder("advancements", "advancements");
    }

    public Path playerDataDir() {
        return playerFolder("playerdata", "data");
    }

    public Path levelDatFile() {
        return worldPath().resolve("level.dat");
    }

    public Path latestLogFile() {
        return serverPath().resolve("logs").resolve("latest.log");
    }

    // Recent versions moved per player files under players/, older worlds still use the legacy folders.
    private Path playerFolder(String legacy, String modern) {
        var legacyDir = worldPath().resolve(legacy);
        return Files.isDirectory(legacyDir) ? legacyDir : worldPath().resolve("players").resolve(modern);
    }

    public Path userCacheFile() {
        return serverPath().resolve("usercache.json");
    }

    public Path serverPropertiesFile() {
        return serverPath().resolve("server.properties");
    }

    public record Rcon(
            @DefaultValue("false") boolean enabled,
            @DefaultValue("localhost") String host,
            @DefaultValue("25575") int port,
            @DefaultValue("") String password,
            @DefaultValue("10s") Duration pollInterval,
            @DefaultValue("60s") Duration saveInterval) {
    }
}
