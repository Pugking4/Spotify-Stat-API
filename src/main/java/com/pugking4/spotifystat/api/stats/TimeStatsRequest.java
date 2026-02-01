package com.pugking4.spotifystat.api.stats;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TimeStatsRequest(
        @NotNull TimeMode mode,
        Calendar period,

        @NotNull
        @Min(0)
        Integer offset,

        @Min(1)
        Integer hours
) {
    @AssertTrue(message = "offset must be smaller than hours when hours is provided")
    public boolean isOffsetValid() {
        return hours == null || offset < hours;
    }

    @AssertTrue(message = "calendar mode requires period and forbids hours")
    public boolean isCalendarValid() {
        return mode != TimeMode.CALENDAR
                || (period != null && hours == null);
    }

    @AssertTrue(message = "rolling mode requires hours and forbids period")
    public boolean isRollingValid() {
        return mode != TimeMode.ROLLING
                || (hours != null && period == null);
    }
}

