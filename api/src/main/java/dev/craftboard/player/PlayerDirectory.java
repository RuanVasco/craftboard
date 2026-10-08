package dev.craftboard.player;

import java.util.Optional;
import java.util.UUID;

public interface PlayerDirectory {

    Optional<String> nameOf(UUID uuid);

    void reload();
}
