package com.pugking4.spotifystat.api.stats;

import com.pugking4.spotifystat.common.dto.LocalTimeRange;

public record TimeBlock(LocalTimeRange timeRange, int playCount, long durationTotal) {
}
