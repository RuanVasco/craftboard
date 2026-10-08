package dev.craftboard.player;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class OfflineUuid {

    private OfflineUuid() {
    }

    // Same derivation the server uses when online-mode=false.
    public static UUID of(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }
}
