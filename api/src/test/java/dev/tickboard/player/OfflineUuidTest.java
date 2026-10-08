package dev.tickboard.player;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class OfflineUuidTest {

    @Test
    void matchesServerDerivation() {
        assertThat(OfflineUuid.of("Steve")).isEqualTo(UUID.fromString("5627dd98-e6be-3c21-b8a8-e92344183641"));
    }
}
