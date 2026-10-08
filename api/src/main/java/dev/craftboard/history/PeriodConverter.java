package dev.craftboard.history;

import java.util.Locale;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

// The API takes ?period=week, not ?period=WEEK.
@Component
public class PeriodConverter implements Converter<String, Period> {

    @Override
    public Period convert(String source) {
        return Period.valueOf(source.strip().toUpperCase(Locale.ROOT));
    }
}
