package com.pugking4.spotifystat.api.stats;

import com.pugking4.spotifystat.common.dto.Artist;

public record ArtistPercentage(Artist artist, int playCount, double percentageOfTracks) {
}
