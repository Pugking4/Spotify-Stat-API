package com.pugking4.spotifystat.api.stats;

import com.pugking4.spotifystat.common.dto.Artist;
import com.pugking4.spotifystat.common.dto.Track;

import java.util.List;

public record TimePeriodStatsResponse(
        List<TrackPlayCount> topTracks,
        SingleValueStats allSingleValueStats,
        Track longestTrack,
        Track shortestTrack,
        Session longestListeningSession,
        ListeningTimeHeatmap listeningTimeHeatmap,
        List<ArtistPercentage> artistDistribution,
        Artist mostNicheArtist,
        Artist mostPopularArtist
) { }
