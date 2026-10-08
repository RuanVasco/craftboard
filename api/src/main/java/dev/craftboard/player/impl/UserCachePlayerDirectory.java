package dev.craftboard.player.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import dev.craftboard.config.MinecraftProperties;
import dev.craftboard.player.PlayerDirectory;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCachePlayerDirectory implements PlayerDirectory {

    private final MinecraftProperties properties;
    private final ObjectMapper objectMapper;

    // Entries expire from usercache.json, so names are never removed once seen.
    private final Map<UUID, String> names = new ConcurrentHashMap<>();

    @PostConstruct
    @Override
    public void reload() {
        var file = properties.userCacheFile();
        if (!Files.exists(file)) {
            log.warn("usercache.json not found at {}", file.toAbsolutePath());
            return;
        }
        try {
            List<UserCacheEntry> entries = objectMapper.readValue(Files.readAllBytes(file), new TypeReference<>() {
            });
            entries.forEach(entry -> names.put(entry.uuid(), entry.name()));
        } catch (IOException | JacksonException e) {
            log.warn("Could not read {}: {}", file, e.getMessage());
        }
    }

    @Override
    public Optional<String> nameOf(UUID uuid) {
        return Optional.ofNullable(names.get(uuid));
    }

    record UserCacheEntry(String name, UUID uuid, String expiresOn) {
    }
}
