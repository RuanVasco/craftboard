package dev.craftboard.stats;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.craftboard.history.HistoryService;
import dev.craftboard.history.Period;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
public class StatsController {

    private final HistoryService historyService;
    private final StatsService statsService;

    @GetMapping
    public List<PlayerStats> list(@RequestParam(defaultValue = "all") Period period) {
        return historyService.ranking(period);
    }

    /** Every stat of one player, by category and key. */
    @GetMapping("/{uuid}/stats")
    public ResponseEntity<Map<String, Map<String, Long>>> stats(@PathVariable UUID uuid) {
        return ResponseEntity.of(Optional.ofNullable(statsService.raw().get(uuid)).map(RawStats::categories));
    }
}
