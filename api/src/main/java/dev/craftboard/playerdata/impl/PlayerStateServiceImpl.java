package dev.craftboard.playerdata.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import dev.craftboard.config.MinecraftProperties;
import dev.craftboard.playerdata.PlayerDataParser;
import dev.craftboard.playerdata.PlayerState;
import dev.craftboard.playerdata.PlayerStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerStateServiceImpl implements PlayerStateService {

    private final MinecraftProperties properties;
    private final PlayerDataParser parser;

    @Override
    public Optional<PlayerState> of(UUID uuid) {
        var file = properties.playerDataDir().resolve(uuid + ".dat");
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(parser.parse(Files.readAllBytes(file), Files.getLastModifiedTime(file).toInstant()));
        } catch (IOException e) {
            // Usually a save caught halfway, the next request reads the finished file.
            log.debug("Could not read {}: {}", file, e.getMessage());
            return Optional.empty();
        }
    }
}
