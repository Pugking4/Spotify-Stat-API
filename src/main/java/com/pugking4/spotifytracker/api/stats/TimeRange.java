package com.[REDACTED].spotifytracker.api.stats;

import java.time.LocalTime;

public class TimeRange {
    public LocalTime startTime;
    public LocalTime endTime;

    public TimeRange(LocalTime startTime, LocalTime endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public boolean isWithin(LocalTime time) {
        return startTime.isBefore(time) && endTime.isAfter(time);
    }
}
