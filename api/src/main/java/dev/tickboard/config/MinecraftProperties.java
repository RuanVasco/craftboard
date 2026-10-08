package dev.tickboard.config;

import java.nio.file.Path;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("tickboard")
public record MinecraftProperties(
        @DefaultValue("../dev-data") Path serverDir,
        @DefaultValue("world") String levelName,
        @DefaultValue("localhost") String publicAddress,
        @DefaultValue Rcon rcon) {

    public Path statsDir() {
        return serverDir.resolve(levelName).resolve("stats");
    }

    public Path userCacheFile() {
        return serverDir.resolve("usercache.json");
    }

    public Path serverPropertiesFile() {
        return serverDir.resolve("server.properties");
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
