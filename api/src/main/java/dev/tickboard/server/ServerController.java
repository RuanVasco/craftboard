package dev.tickboard.server;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ServerController {

    private final ServerService serverService;

    @GetMapping("/api/server")
    public ServerInfo info() {
        return serverService.info();
    }
}
