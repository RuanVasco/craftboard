package dev.craftboard.stats;

import java.time.Instant;
import java.util.UUID;

public record PlayerStats(
        UUID uuid,
        String name,
        long deaths,
        long playTimeTicks,
        long mobKills,
        long playerKills,
        long distanceCm,
        long blocksMined,
        Instant updatedAt) {

    public static PlayerStats of(UUID uuid, String name, RawStats raw, Instant updatedAt) {
        return new PlayerStats(uuid, name, raw.deaths(), raw.playTimeTicks(), raw.mobKills(), raw.playerKills(),
                raw.distanceCm(), raw.blocksMined(), updatedAt);
    }
}
