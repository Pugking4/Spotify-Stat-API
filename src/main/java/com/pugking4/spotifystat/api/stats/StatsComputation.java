package com.pugking4.spotifystat.api.stats;

import com.pugking4.spotifystat.common.dto.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

public class StatsComputation {
    private final List<PlayedTrack> allTimePlayedTracks;
    private final List<PlayedTrack> periodPlayedTracks;

    private final List<PlayedTrack> excludedPlayedTrackData;
    private final List<Track> tracks;
    private final List<Album> albums;
    private final List<Artist> artists; //flat
    private final Set<Track> distinctTracks;
    private final Set<Album> distinctAlbums;
    private final Set<Artist> distinctArtists; //flat

    private final Map<Track, Integer> trackPlayCounts;
    private final Map<Artist, Integer> artistPlayCounts;
    private final int totalPlays;

    private final List<Artist> filteredArtists;
    private final List<PlayedTrack> recentlyReleasedTracks;

    private final List<Session> sessions;

    private final String EXCLUDE_ARTIST = "0LyfQWJT6nXafLPZqxe9Of"; // excludes "Various Artists" artist
    private static final int MINIMUM_DAYS_SINCE_RELEASE_POPULARITY = 5;

    private static final int MAX_LISTENING_SESSION_GAP_MINUTES = 15;
    private static final float PERCENTAGE_OF_TRACK_NEEDED_TO_TRACK = 0.70F;
    private static final int TIME_BLOCK_LENGTH_MINUTES = 15; // must be a factor of 60

    public StatsComputation(List<PlayedTrack> periodPlayedTracks, List<PlayedTrack> allTimePlayedTracks) {
        this.periodPlayedTracks = periodPlayedTracks;
        this.allTimePlayedTracks = allTimePlayedTracks;
        this.excludedPlayedTrackData = new ArrayList<>(allTimePlayedTracks);
        this.excludedPlayedTrackData.removeAll(periodPlayedTracks);
        this.tracks = periodPlayedTracks.stream()
                .map(PlayedTrack::track)
                .toList();
        this.albums = periodPlayedTracks.stream()
                .map(x -> x.track().album())
                .toList();
        this.artists = periodPlayedTracks.stream()
                .map(x -> x.track().artists())
                .flatMap(List::stream)
                .toList();
        this.distinctTracks = new HashSet<>(tracks);
        this.distinctAlbums = new HashSet<>(albums);
        this.distinctArtists = new HashSet<>(artists);

        trackPlayCounts = tracks.stream()
                .collect(Collectors.groupingBy(
                        Track::id,
                        Collectors.summingInt(_ -> 1)
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        e -> distinctTracks.stream()
                                .filter(t -> t.id().equals(e.getKey()))
                                .findFirst().orElseThrow(),
                        Map.Entry::getValue
                ));

        artistPlayCounts = artists.stream()
                .collect(Collectors.groupingBy(
                        Artist::id,
                        Collectors.summingInt(_ -> 1)
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        e -> distinctArtists.stream()
                                .filter(t -> t.id().equals(e.getKey()))
                                .findFirst().orElseThrow(),
                        Map.Entry::getValue
                ));

        totalPlays = periodPlayedTracks.size();

        filteredArtists = artists.stream()
                .filter(a -> !a.id().equals(EXCLUDE_ARTIST))
                .toList();

        recentlyReleasedTracks = periodPlayedTracks.stream()
                .filter(this::isTrackRecentlyReleased)
                .toList();

        sessions = calculateSessions(0.05f);
    }

    public List<TrackPlayCount> topTracks(int limit) {
        return trackPlayCounts.entrySet().stream()
                .sorted(Map.Entry.<Track, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(e -> new TrackPlayCount(e.getKey(), e.getValue()))
                .toList();
    }

    private boolean isTrackRecentlyReleased(PlayedTrack track) {
        LocalDate release = track.track().album().releaseDate();
        LocalDate threshold = LocalDate.now().minusDays(MINIMUM_DAYS_SINCE_RELEASE_POPULARITY);
        return !release.isAfter(threshold);
    }

    private SingleValueStats.TrackStats calculateTrackSingleValueStats() {
        int totalUniqueTracksPlayed = distinctTracks.size();
        long totalListeningTime = tracks.stream()
                .mapToLong(Track::durationMs)
                .sum();
        Set<Track> newPlayedTracks = new HashSet<>(tracks);
        newPlayedTracks.removeAll(excludedPlayedTrackData.stream()
                .map(PlayedTrack::track)
                .collect(Collectors.toSet()));
        int totalNewTracksPlayed = newPlayedTracks.size();
        int totalLocalTracksPlayed = Math.toIntExact(tracks.stream()
                .filter(Track::isLocal)
                .count());
        int totalExplicitTracksPlayed = Math.toIntExact(tracks.stream()
                .filter(Track::isExplicit)
                .count());
        long averageTrackDuration = Math.round(tracks.stream()
                .mapToLong(Track::durationMs)
                .average()
                .orElse(-1));
        float averageTrackPopularity = (float) recentlyReleasedTracks.stream()
                .mapToInt(PlayedTrack::currentPopularity)
                .average()
                .orElse(-1);

        return new SingleValueStats.TrackStats(totalPlays, totalUniqueTracksPlayed, totalListeningTime, totalNewTracksPlayed, totalLocalTracksPlayed, totalExplicitTracksPlayed, averageTrackDuration, averageTrackPopularity);
    }

    private SingleValueStats.AlbumStats calculateAlbumSingleValueStats() {
        int totalUniqueAlbumsPlayed = distinctAlbums.size();

        return new SingleValueStats.AlbumStats(totalUniqueAlbumsPlayed);
    }

    private SingleValueStats.ArtistStats calculateArtistSingleValueStats() {
        int totalUniqueArtistsPlayed = distinctArtists.size();

        return new SingleValueStats.ArtistStats(totalUniqueArtistsPlayed);
    }

    private SingleValueStats.MiscStats calculateMiscSingleValueStats() {
        int totalListeningSessions = sessions.size();
        long averageListeningSessionLength = Math.round(sessions.stream()
                .mapToLong(x -> x.period().getDurationMs())
                .average()
                .orElse(-1));
        float averagePercentageListeningToMusicDuringSession = (float) sessions.stream()
                .mapToDouble(
                        x -> (double) x.playedTracks().stream()
                                .mapToLong(y -> y.track().durationMs())
                                .sum() / x.period().getDurationMs())
                .average()
                .orElse(-1);
        int listeningStreak = calculateListeningStreakInDays();

        Set<Track> nonLikedSongs = periodPlayedTracks.stream()
                .filter(x -> x.contextType().equals("playlist") || x.contextType().equals("album"))
                .map(PlayedTrack::track)
                .collect(Collectors.toSet());
        Set<Track> likedSongs = periodPlayedTracks.stream()
                .filter(x -> x.contextType().equals("collection"))
                .map(PlayedTrack::track)
                .collect(Collectors.toSet());
        long nonLikedAddedCount = nonLikedSongs.stream()
                .filter(likedSongs::contains)
                .count();
        float percentageOfSongsAddedToLikedAfterPlay;
        if (nonLikedSongs.isEmpty()) {
            percentageOfSongsAddedToLikedAfterPlay = -1;
        } else {
            percentageOfSongsAddedToLikedAfterPlay = (float) nonLikedAddedCount / nonLikedSongs.size();
        }

        Set<Track> allLikedSongs = allTimePlayedTracks.stream()
                .filter(x -> x.contextType().equals("collection"))
                .map(PlayedTrack::track)
                .collect(Collectors.toSet());
        int totalNewTracksAddedToLiked = Math.toIntExact(likedSongs.stream()
                .filter(allLikedSongs::contains)
                .count());

        return new SingleValueStats.MiscStats(totalListeningSessions, averageListeningSessionLength, averagePercentageListeningToMusicDuringSession, listeningStreak, percentageOfSongsAddedToLikedAfterPlay, totalNewTracksAddedToLiked);
    }

    public SingleValueStats calculateAllSingleValueStats() {
        return new SingleValueStats(calculateTrackSingleValueStats(), calculateAlbumSingleValueStats(), calculateArtistSingleValueStats(), calculateMiscSingleValueStats());
    }

    private List<Session> calculateSessions(float timingFuzzPercentage) {
        if (periodPlayedTracks.isEmpty()) return Collections.emptyList();

        List<PlayedTrack> playedTracksSorted = new ArrayList<>(periodPlayedTracks);
        playedTracksSorted.sort(Comparator.comparing(PlayedTrack::timeFinished));

        List<Session> sessions = new ArrayList<>();
        List<PlayedTrack> currentSessionPlayedTracks = new ArrayList<>();
        Instant previousSong = playedTracksSorted.getFirst().timeFinished();

        for (PlayedTrack playedTrack : playedTracksSorted) {
            Instant currentSong = playedTrack.timeFinished();
            if (currentSong.isAfter(previousSong.plusSeconds(MAX_LISTENING_SESSION_GAP_MINUTES * 60))) {
                if (!currentSessionPlayedTracks.isEmpty()) {
                    PlayedTrack startingTrack = currentSessionPlayedTracks.getFirst();
                    PlayedTrack endingTrack =  currentSessionPlayedTracks.getLast();
                    sessions.add(new Session( new InstantTimeRange(
                            startingTrack.timeFinished().minusMillis(
                                        Math.round(startingTrack.track().durationMs() * (PERCENTAGE_OF_TRACK_NEEDED_TO_TRACK - timingFuzzPercentage))
                                ),
                            endingTrack.timeFinished().plusMillis(
                                        Math.round(endingTrack.track().durationMs() * ((1 - PERCENTAGE_OF_TRACK_NEEDED_TO_TRACK) - timingFuzzPercentage))
                                )
                    ),
                            new ArrayList<>(currentSessionPlayedTracks)));
                    currentSessionPlayedTracks.clear();
                }
            }
            currentSessionPlayedTracks.add(playedTrack);
            previousSong = currentSong;
        }

        if (!currentSessionPlayedTracks.isEmpty()) {
            sessions.add(new Session( new InstantTimeRange(
                    currentSessionPlayedTracks.getFirst().timeFinished(),
                    currentSessionPlayedTracks.getLast().timeFinished()
            ),

                    currentSessionPlayedTracks
            ));
        }

        return sessions;
    }

    public Track findLongestTrack() {
        return distinctTracks.stream()
                .max(Comparator.comparingLong(Track::durationMs))
                .orElse(null);
    }

    public Track findShortestTrack() {
        return distinctTracks.stream()
                .min(Comparator.comparingLong(Track::durationMs))
                .orElse(null);
    }

    private int calculateListeningStreakInDays() {
        ZoneId aus = ZoneId.of("Australia/Sydney");
        LocalDate checkingDate = LocalDate.now(aus);

        boolean dayFound;
        int streak = 0;

        do {
            LocalDate dateForLambda = checkingDate;
            dayFound = allTimePlayedTracks.stream()
                    .anyMatch(x -> {
                        LocalDate playedDate = x.timeFinished().atZone(aus).toLocalDate();
                        return playedDate.isEqual(dateForLambda);
                    });
            checkingDate = checkingDate.minusDays(1);
            if (dayFound) streak++;
        } while (dayFound);

        return streak;
    }

    public Session findLongestListeningSession() {
        return sessions.stream()
                .max(Comparator.comparingLong(x -> x.period().getDurationMs()))
                .orElse(null);

    }

    private List<LocalTimeRange> generateTimeRanges() {
        List<LocalTimeRange> timeBlocks = new ArrayList<>();
        for (int i = 1; i < 1 + (60 / TIME_BLOCK_LENGTH_MINUTES) * 24; i++) {
            LocalTime startTime = LocalTime.MIDNIGHT.plusMinutes(TIME_BLOCK_LENGTH_MINUTES * (i - 1));
            LocalTime endTime = LocalTime.MIDNIGHT.plusMinutes(TIME_BLOCK_LENGTH_MINUTES * i);
            timeBlocks.add(new LocalTimeRange(startTime, endTime));
        }
        return timeBlocks;
    }

    public ListeningTimeHeatmap calculateListeningTimeHeatmap() {
        List<LocalTimeRange> timeRanges = generateTimeRanges();
        List<TimeBlock> heatmap = new ArrayList<>();
        ZoneId aus = ZoneId.of("Australia/Sydney");

        for (LocalTimeRange range : timeRanges) {
            List<PlayedTrack> timeBlockPlayedTracks = periodPlayedTracks.stream()
                    .filter(x -> range.isWithin(LocalTime.ofInstant(x.timeFinished(), aus)))
                    .toList();
            int playCount = timeBlockPlayedTracks.size();
            long durationTotal = timeBlockPlayedTracks.stream().mapToLong(x -> x.track().durationMs()).sum();
            TimeBlock tb = new TimeBlock(range, playCount, durationTotal);
            heatmap.add(tb);
        }

        int maxPlayCount = Math.toIntExact(heatmap.stream()
                .mapToLong(TimeBlock::playCount)
                .max()
                .orElse(-1));

        long maxDurationTotal = heatmap.stream()
                .mapToLong(TimeBlock::durationTotal)
                .max()
                .orElse(-1);

        return new ListeningTimeHeatmap(heatmap, 0, maxPlayCount, 0, maxDurationTotal);
    }

    public List<ArtistPercentage> calculateArtistDistribution() {
        return artistPlayCounts.entrySet().stream()
                .map( apc -> {
                    int playCount = apc.getValue();
                    double percentage = (double) playCount /  totalPlays;
                    return new ArtistPercentage(apc.getKey(), playCount, percentage);
                })
                .sorted(Comparator.comparingDouble(ArtistPercentage::percentageOfTracks))
                .toList()
                .reversed();
    }

    public Artist findMostNicheArtist() {
        int minPop = filteredArtists.stream()
                .mapToInt(Artist::popularity)
                .min()
                .orElse(-1);

        return filteredArtists.stream()
                .filter(a -> a.popularity() == minPop)
                .min(Comparator.comparingInt(Artist::followers))
                .orElse(null);
    }

    public Artist findMostPopularArtist() {
        int maxPop = filteredArtists.stream()
                .mapToInt(Artist::popularity)
                .max()
                .orElse(-1);

        return filteredArtists.stream()
                .filter(a -> a.popularity() == maxPop)
                .max(Comparator.comparingInt(Artist::followers))
                .orElse(null);
    }

    public PlayedTrack findMostPopularTrack() {
        return recentlyReleasedTracks.stream()
                .max(Comparator.comparingInt(PlayedTrack::currentPopularity))
                .orElse(null);
    }

    public PlayedTrack findMostNicheTrack() {
        return recentlyReleasedTracks.stream()
                .min(Comparator.comparingInt(PlayedTrack::currentPopularity))
                .orElse(null);
    }
}

