package dev.craftboard.world.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

import org.springframework.stereotype.Service;

import dev.craftboard.config.MinecraftProperties;
import dev.craftboard.world.LevelDatParser;
import dev.craftboard.world.WorldInfo;
import dev.craftboard.world.WorldService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorldServiceImpl implements WorldService {

    private final MinecraftProperties properties;
    private final LevelDatParser parser;

    @Override
    public Optional<WorldInfo> info() {
        var file = properties.levelDatFile();
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(parser.parse(Files.readAllBytes(file), Files.getLastModifiedTime(file).toInstant()));
        } catch (IOException e) {
            // The server rewrites level.dat on every save, a read can land in the middle of it.
            log.debug("Could not read {}: {}", file, e.getMessage());
            return Optional.empty();
        }
    }
}
