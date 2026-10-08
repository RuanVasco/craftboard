package dev.craftboard.history;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;

    @GetMapping("/api/players/{uuid}/history")
    public List<SnapshotPoint> history(
            @PathVariable UUID uuid,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        return historyService.history(uuid, from, to);
    }
}
