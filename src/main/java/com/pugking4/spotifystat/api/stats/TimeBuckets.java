package com.pugking4.spotifystat.api.stats;

import java.time.Instant;

public final class TimeBuckets {

    private static final long SCRAPE_SEC = 15;

    private TimeBuckets() {}

    public static Instant alignEnd(Instant end, int bucketMinutes) {
        long step = bucketMinutes * 60L;
        long aligned = (end.getEpochSecond() / step) * step;
        return Instant.ofEpochSecond(aligned);
    }

    public static long clampRange(long seconds) {
        return Math.max(seconds, 2 * SCRAPE_SEC);
    }
}

