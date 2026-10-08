package dev.craftboard.playerdata;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class PlayerStateController {

    private final PlayerStateService playerStateService;

    @GetMapping("/api/players/{uuid}/state")
    public ResponseEntity<PlayerState> state(@PathVariable UUID uuid) {
        return ResponseEntity.of(playerStateService.of(uuid));
    }
}
