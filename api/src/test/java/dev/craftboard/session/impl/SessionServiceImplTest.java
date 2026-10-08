package dev.craftboard.session.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import dev.craftboard.IntegrationTest;
import dev.craftboard.player.OfflineUuid;
import dev.craftboard.player.PlayerRepository;
import dev.craftboard.session.PlayerSession;
import dev.craftboard.session.PlayerSessionRepository;

class SessionServiceImplTest extends IntegrationTest {

    @Autowired
    private SessionServiceImpl sessionService;
    @Autowired
    private PlayerSessionRepository sessions;
    @Autowired
    private PlayerRepository players;

    @Test
    void opensSessionsForPlayersThatJoined() {
        sessionService.reconcile(List.of("Steve", "Alex"));

        assertThat(sessions.findByLeftAtIsNull())
                .extracting(PlayerSession::getPlayerUuid)
                .containsExactlyInAnyOrder(OfflineUuid.of("Steve"), OfflineUuid.of("Alex"));
        assertThat(players.findById(OfflineUuid.of("Alex"))).get().extracting("name").isEqualTo("Alex");
    }

    @Test
    void closesOnlyTheSessionsOfPlayersThatLeft() {
        sessionService.reconcile(List.of("Steve", "Alex"));

        sessionService.reconcile(List.of("Steve"));

        assertThat(sessions.findByLeftAtIsNull())
                .extracting(PlayerSession::getPlayerUuid)
                .containsExactly(OfflineUuid.of("Steve"));
        assertThat(sessions.count()).isEqualTo(2);
    }

    @Test
    void closesEverythingWhenTheServerGoesOffline() {
        sessionService.reconcile(List.of("Steve", "Alex"));

        sessionService.reconcile(List.of());

        assertThat(sessions.findByLeftAtIsNull()).isEmpty();
        assertThat(sessions.findAll()).allSatisfy(s -> assertThat(s.getLeftAt()).isAfterOrEqualTo(s.getJoinedAt()));
    }

    @Test
    void rejoiningOpensANewSession() {
        sessionService.reconcile(List.of("Steve"));
        sessionService.reconcile(List.of());

        sessionService.reconcile(List.of("Steve"));

        assertThat(sessions.count()).isEqualTo(2);
        assertThat(sessions.findByLeftAtIsNull()).hasSize(1);
    }
}
