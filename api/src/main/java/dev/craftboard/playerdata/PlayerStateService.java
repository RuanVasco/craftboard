package dev.craftboard.playerdata;

import java.util.Optional;
import java.util.UUID;

public interface PlayerStateService {

    Optional<PlayerState> of(UUID uuid);
}
