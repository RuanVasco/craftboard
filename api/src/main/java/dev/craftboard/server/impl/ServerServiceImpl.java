package dev.craftboard.server.impl;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import dev.craftboard.config.MinecraftProperties;
import dev.craftboard.rcon.RconClient;
import dev.craftboard.server.OnlinePlayersPolledEvent;
import dev.craftboard.server.ServerInfo;
import dev.craftboard.server.ServerService;
import dev.craftboard.stream.StreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServerServiceImpl implements ServerService {

    private static final int DEFAULT_MAX_PLAYERS = 20;

    private final MinecraftProperties properties;
    private final RconClient rcon;
    private final StreamService streamService;
    private final ApplicationEventPublisher events;

    private volatile Boolean online;
    private volatile List<String> onlinePlayers;

    @Override
    public ServerInfo info() {
        var serverProperties = readServerProperties();
        return new ServerInfo(
                properties.publicAddress(),
                serverProperties.getProperty("motd", "A Minecraft Server"),
                parseInt(serverProperties.getProperty("max-players"), DEFAULT_MAX_PLAYERS),
                properties.rcon().enabled(),
                online,
                onlinePlayers);
    }

    @Scheduled(fixedDelayString = "${craftboard.rcon.poll-interval:10s}")
    public void pollOnlinePlayers() {
        if (!properties.rcon().enabled()) {
            return;
        }
        Boolean nowOnline;
        List<String> nowPlayers;
        try {
            nowPlayers = parseList(rcon.command("list"));
            nowOnline = true;
        } catch (IOException e) {
            log.debug("RCON list failed: {}", e.getMessage());
            nowPlayers = List.of();
            nowOnline = false;
        }
        if (!Objects.equals(nowOnline, online) || !Objects.equals(nowPlayers, onlinePlayers)) {
            online = nowOnline;
            onlinePlayers = nowPlayers;
            streamService.broadcast("server", info());
        }
        events.publishEvent(new OnlinePlayersPolledEvent(nowPlayers));
    }

    // Stats files only hit the disk on save, so forcing one is what makes the dashboard feel live.
    @Scheduled(fixedDelayString = "${craftboard.rcon.save-interval:60s}")
    public void forceSave() {
        if (!properties.rcon().enabled() || !Boolean.TRUE.equals(online)) {
            return;
        }
        try {
            rcon.command("save-all");
        } catch (IOException e) {
            log.debug("RCON save-all failed: {}", e.getMessage());
        }
    }

    // "There are 2 of a max of 20 players online: Ruan, Steve"
    static List<String> parseList(String response) {
        int colon = response.indexOf(':');
        if (colon < 0) {
            return List.of();
        }
        return Arrays.stream(response.substring(colon + 1).split(","))
                .map(String::strip)
                .filter(name -> !name.isEmpty())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private Properties readServerProperties() {
        var result = new Properties();
        var file = properties.serverPropertiesFile();
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                result.load(reader);
            } catch (IOException e) {
                log.warn("Could not read {}: {}", file, e.getMessage());
            }
        }
        return result;
    }

    private static int parseInt(String value, int fallback) {
        try {
            return value == null ? fallback : Integer.parseInt(value.strip());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
