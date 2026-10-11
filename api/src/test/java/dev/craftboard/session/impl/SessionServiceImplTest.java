package dev.craftboard.session.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.craftboard.IntegrationTest;
import dev.craftboard.player.OfflineUuid;
import dev.craftboard.player.Player;
import dev.craftboard.player.PlayerRepository;
import dev.craftboard.session.PlayerSession;
import dev.craftboard.session.PlayerSessionRepository;
import dev.craftboard.session.SessionLength;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class SessionServiceImplTest extends IntegrationTest {

    private final SessionServiceImpl sessionService;
    private final PlayerSessionRepository sessions;
    private final PlayerRepository players;

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

    @Test
    void longestSessionCountsOpenSessionsUpToNow() {
        var now = Instant.now();
        var steve = players.save(new Player(OfflineUuid.of("Steve"), "Steve", now)).getUuid();
        var alex = players.save(new Player(OfflineUuid.of("Alex"), "Alex", now)).getUuid();
        var closed = new PlayerSession(steve, now.minus(Duration.ofHours(2)));
        closed.setLeftAt(now.minus(Duration.ofHours(1)));
        sessions.save(closed);
        sessions.save(new PlayerSession(alex, now.minus(Duration.ofHours(3))));

        assertThat(sessionService.longest()).get()
                .extracting(SessionLength::playerUuid).isEqualTo(alex);
        assertThat(sessionService.longest()).get()
                .extracting(SessionLength::seconds).satisfies(seconds -> assertThat(seconds).isBetween(3L * 3600, 3L * 3600 + 60));
    }
}
