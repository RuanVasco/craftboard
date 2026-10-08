package dev.craftboard.activity;

import java.time.LocalDate;
import java.util.List;

/**
 * Hours played, in the configured timezone.
 *
 * @param heatmap seven rows (Monday first) of 24 hourly totals, over the whole window
 * @param daily   one entry per day of the window, oldest first, days without play included
 */
public record Activity(int days, double[][] heatmap, List<Day> daily) {

    public record Day(LocalDate date, double hours, int players) {
    }
}
