package dev.craftboard.stream.impl;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import dev.craftboard.stats.StatsUpdatedEvent;
import dev.craftboard.stream.StreamService;

@Service
public class StreamServiceImpl implements StreamService {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @Override
    public SseEmitter subscribe() {
        var emitter = new SseEmitter(0L);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        emitters.add(emitter);
        return emitter;
    }

    @Override
    public void broadcast(String event, Object data) {
        emitters.forEach(emitter -> send(emitter, SseEmitter.event().name(event).data(data)));
    }

    @EventListener
    public void onStatsUpdated(StatsUpdatedEvent event) {
        broadcast("player", event.stats());
    }

    // Proxies (Cloudflare included) drop idle connections after ~100s.
    @Scheduled(fixedRate = 25_000)
    public void heartbeat() {
        emitters.forEach(emitter -> send(emitter, SseEmitter.event().comment("ping")));
    }

    private void send(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException e) {
            emitters.remove(emitter);
        }
    }
}
