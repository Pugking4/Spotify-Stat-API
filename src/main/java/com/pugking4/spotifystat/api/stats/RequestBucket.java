package com.pugking4.spotifystat.api.stats;

public record RequestBucket(
        long timeEnd,
        boolean complete,
        double success,
        double clientError,
        double serverError
) {}
