package dev.craftboard.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("craftboard")
public record MinecraftProperties(
        @DefaultValue("../dev-data") String serverDir,
        @DefaultValue("world") String levelName,
        @DefaultValue("localhost") String publicAddress,
        @DefaultValue Rcon rcon) {

    public Path serverPath() {
        return Path.of(serverDir);
    }

    // Recent versions moved stats under players/, older worlds still use the legacy folder.
    public Path statsDir() {
        var world = serverPath().resolve(levelName);
        var legacy = world.resolve("stats");
        return Files.isDirectory(legacy) ? legacy : world.resolve("players").resolve("stats");
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
