package com.pugking4.spotifystat.api.stats;

public record LatencyBucket(
        long timeEnd,
        boolean complete,
        long rangeSec,
        double p50Ms,
        double p95Ms,
        double p99Ms
) {}
