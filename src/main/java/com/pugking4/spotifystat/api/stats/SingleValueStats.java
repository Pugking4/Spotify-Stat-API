package com.pugking4.spotifystat.api.stats;

public record SingleValueStats(TrackStats track, AlbumStats album, ArtistStats artist, MiscStats misc) {
    record TrackStats(int totalTracksPlayed, int totalUniqueTracksPlayed, long totalListeningTime, int totalNewTracksPlayed, int totalLocalTracksPlayed, int totalExplicitTracksPlayed, long averageTrackDuration, float averageTrackPopularity) {}
    record AlbumStats(int totalUniqueAlbumsPlayed) {}
    record ArtistStats(int totalUniqueArtistsPlayed) {}
    record MiscStats(int totalListeningSessions, double averageListeningSessionLength, float averagePercentageListeningToMusicDuringSession, int currentListeningStreak, float percentageTracksAddedToLikedAfterPlay, int totalNewTracksAddedToLiked) {}
}