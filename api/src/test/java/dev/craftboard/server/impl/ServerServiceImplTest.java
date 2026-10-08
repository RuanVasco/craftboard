package dev.craftboard.server.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ServerServiceImplTest {

    @Test
    void parsesOnlinePlayers() {
        assertThat(ServerServiceImpl.parseList("There are 2 of a max of 20 players online: Steve, alex"))
                .containsExactly("alex", "Steve");
    }

    @Test
    void parsesEmptyServer() {
        assertThat(ServerServiceImpl.parseList("There are 0 of a max of 20 players online: ")).isEmpty();
    }
}
