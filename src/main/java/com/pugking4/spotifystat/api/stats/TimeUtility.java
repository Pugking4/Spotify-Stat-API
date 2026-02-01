package com.pugking4.spotifystat.api.stats;

import com.pugking4.spotifystat.api.data.Pair;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TimeUtility {
    public static Pair<LocalDateTime, LocalDateTime> getTrackingPeriod(Calendar mode, int offset) {
        return switch(mode) {
            case DAY -> new Pair<>(LocalDate.now().minusDays(offset).atStartOfDay(), LocalDate.now().minusDays(offset + 1).atStartOfDay());
            case WEEK -> new Pair<>(LocalDate.now().minusWeeks(offset).with(DayOfWeek.MONDAY).atStartOfDay(), LocalDate.now().minusWeeks(offset + 1).with(DayOfWeek.MONDAY).atStartOfDay());
            case MONTH -> new Pair<>(LocalDate.now().minusMonths(offset).withDayOfMonth(1).atStartOfDay(), LocalDate.now().minusMonths(offset + 1).withDayOfMonth(1).atStartOfDay());
            case YEAR -> new Pair<>(LocalDate.now().minusYears(offset).withDayOfYear(1).atStartOfDay(), LocalDate.now().minusYears(offset + 1).withDayOfYear(1).atStartOfDay());
        };
    }

    public static Pair<LocalDateTime, LocalDateTime> getTrackingPeriod(int hours, int offset) {
        LocalDateTime end = LocalDateTime.now().minusHours(offset);
        LocalDateTime start = end.minusHours(hours);
        return new Pair<>(start, end);
    }
}
