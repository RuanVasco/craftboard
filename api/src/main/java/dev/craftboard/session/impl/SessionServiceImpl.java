package dev.craftboard.session.impl;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import dev.craftboard.player.OfflineUuid;
import dev.craftboard.player.Player;
import dev.craftboard.player.PlayerRepository;
import dev.craftboard.server.OnlinePlayersChangedEvent;
import dev.craftboard.session.PlayerSession;
import dev.craftboard.session.PlayerSessionRepository;
import dev.craftboard.session.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final PlayerRepository players;
    private final PlayerSessionRepository sessions;
    private final TransactionTemplate transaction;

    @EventListener
    public void onOnlinePlayersChanged(OnlinePlayersChangedEvent event) {
        try {
            reconcile(event.onlinePlayers());
        } catch (DataAccessException e) {
            log.warn("Could not record sessions: {}", e.getMessage());
        }
    }

    @Override
    public synchronized void reconcile(List<String> onlinePlayers) {
        transaction.executeWithoutResult(status -> {
            var now = Instant.now();
            Map<UUID, String> online = new HashMap<>();
            onlinePlayers.forEach(name -> online.put(OfflineUuid.of(name), name));

            for (var session : sessions.findByLeftAtIsNull()) {
                if (online.remove(session.getPlayerUuid()) == null) {
                    session.setLeftAt(now);
                    players.findById(session.getPlayerUuid()).ifPresent(p -> p.setLastSeen(now));
                }
            }
            online.forEach((uuid, name) -> {
                var player = players.findById(uuid).orElseGet(() -> players.save(new Player(uuid, name, now)));
                player.setLastSeen(now);
                sessions.save(new PlayerSession(uuid, now));
            });
        });
    }
}
