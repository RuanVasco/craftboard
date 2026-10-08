package dev.craftboard.activity;

import java.util.Optional;
import java.util.UUID;

public interface ActivityService {

    /** Whole server activity over the last {@code days}. */
    Activity server(int days);

    Activity player(UUID uuid, int days);

    Optional<PlayerSummary> summary(UUID uuid);
}
