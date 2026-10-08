package dev.craftboard.stats;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping
    public List<PlayerStats> list(@RequestParam(defaultValue = "all") Period period) {
        return historyService.ranking(period);
    }
}
