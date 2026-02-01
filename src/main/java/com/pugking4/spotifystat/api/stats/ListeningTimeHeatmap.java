package com.pugking4.spotifystat.api.stats;

import java.util.List;

public record ListeningTimeHeatmap(List<TimeBlock> heatmap, int minPlayCount, int maxPlayCount, long minDurationTotal, long maxDurationTotal) {
}
