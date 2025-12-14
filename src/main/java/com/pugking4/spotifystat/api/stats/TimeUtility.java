package com.[REDACTED].spotifystat.api.stats;

import com.[REDACTED].spotifystat.api.data.Pair;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TimeUtility {
    public static Pair<LocalDateTime, LocalDateTime> getTrackingPeriod(Calendar mode) {
        return switch(mode) {
            case THIS_DAY -> new Pair<>(LocalDateTime.now(), LocalDate.now().atStartOfDay());
            case THIS_WEEK -> new Pair<>(LocalDateTime.now(), LocalDate.now().with(java.time.DayOfWeek.MONDAY).atStartOfDay());
            case THIS_MONTH -> new Pair<>(LocalDateTime.now(), LocalDate.now().withDayOfMonth(1).atStartOfDay());
            case THIS_YEAR -> new Pair<>(LocalDateTime.now(), LocalDate.now().withDayOfYear(1).atStartOfDay());
            case DAY -> new Pair<>(LocalDate.now().atStartOfDay(), LocalDate.now().minusDays(1).atStartOfDay());
            case WEEK -> new Pair<>(LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay(), LocalDate.now().minusWeeks(1).with(java.time.DayOfWeek.MONDAY).atStartOfDay());
            case MONTH -> new Pair<>(LocalDate.now().withDayOfMonth(1).atStartOfDay(), LocalDate.now().minusMonths(1).withDayOfMonth(1).atStartOfDay());
            case YEAR -> new Pair<>(LocalDate.now().withDayOfYear(1).atStartOfDay(), LocalDate.now().minusYears(1).withDayOfYear(1).atStartOfDay());
        };
    }

    public static Pair<LocalDateTime, LocalDateTime> getTrackingPeriod(int hours) {
        return new Pair<>(LocalDateTime.now(), LocalDateTime.now().minusHours(hours));
    }
}
