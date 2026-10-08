package dev.tickboard.player;

import java.util.Optional;
import java.util.UUID;

public interface PlayerDirectory {

    Optional<String> nameOf(UUID uuid);

    void reload();
}
