package com.pugking4.spotifystat.api.stats;

import java.util.List;

public record RequestsPerMinuteResponse(
        int bucketMinutes,
        List<RequestBucket> buckets
) {}
