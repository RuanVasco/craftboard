package dev.craftboard.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MinecraftPropertiesTest {

    @TempDir
    Path serverDir;

    @Test
    void usesPlayersStatsFolderOnRecentWorlds() throws IOException {
        var stats = Files.createDirectories(serverDir.resolve("world/players/stats"));

        assertThat(properties().statsDir()).isEqualTo(stats);
    }

    @Test
    void keepsLegacyStatsFolderWhenPresent() throws IOException {
        var stats = Files.createDirectories(serverDir.resolve("world/stats"));

        assertThat(properties().statsDir()).isEqualTo(stats);
    }

    @Test
    void resolvesPlayerDataUnderPlayersOnRecentWorlds() throws IOException {
        var data = Files.createDirectories(serverDir.resolve("world/players/data"));

        assertThat(properties().playerDataDir()).isEqualTo(data);
        assertThat(properties().advancementsDir()).isEqualTo(serverDir.resolve("world/players/advancements"));
    }

    @Test
    void keepsLegacyPlayerDataFolderWhenPresent() throws IOException {
        var data = Files.createDirectories(serverDir.resolve("world/playerdata"));

        assertThat(properties().playerDataDir()).isEqualTo(data);
    }

    private MinecraftProperties properties() {
        return new MinecraftProperties(serverDir.toString(), "world", "localhost", ZoneId.of("UTC"), null, null);
    }
}
