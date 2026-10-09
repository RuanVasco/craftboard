package dev.craftboard.ranking;

import java.util.Locale;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

// The API takes ?sort=playTime, not ?sort=PLAY_TIME.
@Component
public class RankingSortConverter implements Converter<String, RankingSort> {

    @Override
    public RankingSort convert(String source) {
        var constant = source.strip().replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
        return RankingSort.valueOf(constant);
    }
}
