package com.pugking4.spotifystat.api.stats;

import java.util.List;
import java.util.Map;

public record TimePeriodStatsResponse(
        List<Map<String, Object>> topTracks,
        Map<String, Object> singleValueStats,
        Map<String, Object> longestTrack,
        Map<String, Object> shortestTrack,
        Map<String, Object> longestListeningSession,
        Map<String, Object> listeningTimeHeatmap,
        List<Map<String, Object>> artistDistribution,
        Map<String, Object> mostNicheArtist,
        Map<String, Object> mostPopularArtist
) {}
