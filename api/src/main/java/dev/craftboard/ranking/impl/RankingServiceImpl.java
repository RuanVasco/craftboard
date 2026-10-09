package dev.craftboard.ranking.impl;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToLongFunction;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;

import dev.craftboard.advancement.AdvancementService;
import dev.craftboard.history.HistoryService;
import dev.craftboard.history.Period;
import dev.craftboard.ranking.RankingPage;
import dev.craftboard.ranking.RankingService;
import dev.craftboard.ranking.RankingSort;
import dev.craftboard.stats.PlayerStats;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {

    static final int MAX_PAGE_SIZE = 100;

    private final HistoryService historyService;
    private final AdvancementService advancementService;

    @Override
    public RankingPage ranking(Period period, RankingSort sort, String query, int page, int size) {
        int pageSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        int pageIndex = Math.max(0, page);

        var players = historyService.ranking(period);
        // Counts are lifetime totals, so outside ALL they are only read when sorting by them.
        Map<UUID, Integer> advancements = period == Period.ALL || sort == RankingSort.ADVANCEMENTS
                ? advancementService.overview(0).counts()
                : Map.of();
        ToLongFunction<PlayerStats> value = sort.stat() != null
                ? sort.stat()
                : p -> advancements.getOrDefault(p.uuid(), 0);

        var sorted = players.stream()
                .sorted(Comparator.comparingLong(value).reversed().thenComparing(PlayerStats::name))
                .toList();
        var needle = normalize(query);
        List<RankingPage.Entry> matching = IntStream.range(0, sorted.size())
                .filter(i -> needle.isEmpty() || matches(sorted.get(i), needle))
                .mapToObj(i -> new RankingPage.Entry(i + 1, sorted.get(i),
                        advancements.getOrDefault(sorted.get(i).uuid(), 0)))
                .toList();

        int from = (int) Math.min((long) pageIndex * pageSize, matching.size());
        int to = Math.min(from + pageSize, matching.size());
        int totalPages = (matching.size() + pageSize - 1) / pageSize;
        long maxValue = sorted.isEmpty() ? 0 : value.applyAsLong(sorted.getFirst());
        return new RankingPage(matching.subList(from, to), pageIndex, pageSize, matching.size(), totalPages,
                maxValue, RankingPage.Totals.of(players));
    }

    // Dashes are dropped so a UUID matches whether it is pasted with or without them.
    private static String normalize(String text) {
        return text == null ? "" : text.strip().toLowerCase(Locale.ROOT).replace("-", "");
    }

    private static boolean matches(PlayerStats player, String needle) {
        return normalize(player.name()).contains(needle) || normalize(player.uuid().toString()).contains(needle);
    }
}
