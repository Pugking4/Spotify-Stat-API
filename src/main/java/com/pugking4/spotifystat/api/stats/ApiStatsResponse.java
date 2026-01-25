package com.pugking4.spotifystat.api.stats;

import java.util.Map;

public record ApiStatsResponse(
        double uptimeSeconds,
        long totalRequests,
        String version,
        String hostname,
        RequestsPerMinuteResponse rollingRequests1m,
        RequestsPerMinuteResponse rollingRequests5m,
        LatencyResponse latency1m,
        LatencyResponse latency5m,
        Map<String, Double> uptimePercentage
) {}
