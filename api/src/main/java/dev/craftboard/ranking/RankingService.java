package dev.craftboard.ranking;

import dev.craftboard.history.Period;

public interface RankingService {

    /**
     * Players of the period ordered by {@code sort}, highest first, filtered by {@code query} (part of
     * the name or of the UUID, dashes optional) and cut to one page.
     */
    RankingPage ranking(Period period, RankingSort sort, String query, int page, int size);
}
