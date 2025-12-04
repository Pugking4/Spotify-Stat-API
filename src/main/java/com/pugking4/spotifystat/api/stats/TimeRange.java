package com.pugking4.spotifystat.api.stats;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

public record TimeRange(LocalTime startTime, LocalTime endTime) {
    public boolean isWithin(LocalTime time) {
        return startTime.isBefore(time) && endTime.isAfter(time);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("startTime", startTime);
        map.put("endTime", endTime);
        return map;
    }
}
