package dev.craftboard.history;

import java.time.Duration;

public enum Period {
    WEEK(Duration.ofDays(7)),
    MONTH(Duration.ofDays(30)),
    ALL(null);

    private final Duration length;

    Period(Duration length) {
        this.length = length;
    }

    public Duration length() {
        return length;
    }
}
