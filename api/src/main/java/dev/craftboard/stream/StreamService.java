package dev.craftboard.stream;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface StreamService {

    SseEmitter subscribe();

    void broadcast(String event, Object data);
}
