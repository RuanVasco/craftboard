package dev.craftboard.activity;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ActivityController {

    private static final int MAX_DAYS = 365;

    private final ActivityService activityService;

    @GetMapping("/api/activity")
    public Activity server(@RequestParam(defaultValue = "30") int days) {
        return activityService.server(Math.clamp(days, 1, MAX_DAYS));
    }

    @GetMapping("/api/players/{uuid}/activity")
    public Activity player(@PathVariable UUID uuid, @RequestParam(defaultValue = "30") int days) {
        return activityService.player(uuid, Math.clamp(days, 1, MAX_DAYS));
    }

    @GetMapping("/api/players/{uuid}/summary")
    public ResponseEntity<PlayerSummary> summary(@PathVariable UUID uuid) {
        return ResponseEntity.of(activityService.summary(uuid));
    }
}
