package com.pugking4.spotifystat.api.stats;

import java.util.List;

public record LatencyResponse(
        int bucketMinutes,
        List<LatencyBucket> buckets
) {}
