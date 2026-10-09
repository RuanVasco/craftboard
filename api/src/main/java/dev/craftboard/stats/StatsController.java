package dev.craftboard.stats;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.craftboard.history.Period;
import dev.craftboard.ranking.RankingPage;
import dev.craftboard.ranking.RankingService;
import dev.craftboard.ranking.RankingSort;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
public class StatsController {

    private final RankingService rankingService;
    private final StatsService statsService;

    @GetMapping
    public RankingPage list(
            @RequestParam(defaultValue = "all") Period period,
            @RequestParam(defaultValue = "playTime") RankingSort sort,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return rankingService.ranking(period, sort, q, page, size);
    }

    /** Every stat of one player, by category and key. */
    @GetMapping("/{uuid}/stats")
    public ResponseEntity<Map<String, Map<String, Long>>> stats(@PathVariable UUID uuid) {
        return ResponseEntity.of(Optional.ofNullable(statsService.raw().get(uuid)).map(RawStats::categories));
    }
}
