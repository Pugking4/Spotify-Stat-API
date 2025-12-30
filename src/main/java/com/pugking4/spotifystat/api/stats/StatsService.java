package com.[REDACTED].spotifystat.api.stats;

import com.[REDACTED].spotifystat.common.dto.*;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

public class StatsService {
    private final List<PlayedTrack> allPlayedTrackData;
    private final List<PlayedTrack> playedTrackData;
    private final List<PlayedTrack> excludedPlayedTrackData;
    private final List<Track> trackData;
    private final List<Album> albumData;
    private final List<Artist> artistData; //flat
    private final Set<Track> distinctTrackData;
    private final Set<Album> distinctAlbumData;
    private final Set<Artist> distinctArtistData; //flat
    private final String EXCLUDE_ARTIST = "0LyfQWJT6nXafLPZqxe9Of"; // excludes "Various Artists" artist
    private final int MINIMUM_DAYS_SINCE_RELEASE_POPULARITY = 5;

    private final int MAX_LISTENING_SESSION_GAP_MINUTES = 15;
    private final int TIME_BLOCK_LENGTH_MINUTES = 15; // must be a factor of 60

    public StatsService(List<PlayedTrack> timePeriodData, List<PlayedTrack> allTimeData) {
        this.playedTrackData = timePeriodData;
        this.allPlayedTrackData = allTimeData;
        this.excludedPlayedTrackData = new ArrayList<>(allPlayedTrackData);
        this.excludedPlayedTrackData.removeAll(playedTrackData);
        this.trackData = playedTrackData.stream()
                .map(PlayedTrack::track)
                .toList();
        this.albumData = playedTrackData.stream()
                .map(x -> x.track().album())
                .toList();
        this.artistData = playedTrackData.stream()
                .map(x -> x.track().artists())
                .flatMap(List::stream)
                .toList();
        this.distinctTrackData = new HashSet<>(trackData);
        this.distinctAlbumData = new HashSet<>(albumData);
        this.distinctArtistData = new HashSet<>(artistData);
    }

    public List<Map<String, Object>> getTopFiveTracks() {
        Map<Track, Integer> countedTracks = distinctTrackData.stream()
                .collect(Collectors.toMap(
                        track -> track,
                        track -> (int) trackData.stream()
                                .filter(x -> x.id().equals(track.id()))
                                .count(),
                        (existing, replacement) -> existing
                ));

        List<Map.Entry<Track, Integer>> finalCountedTracks = countedTracks.entrySet().stream()
                .sorted(Map.Entry.<Track, Integer>comparingByValue().reversed())
                .limit(5)
                .toList();

        return finalCountedTracks.stream()
                .map(entry -> {
                    Map<String, Object> map = entry.getKey().toMap();
                    map.put("play_count", entry.getValue());
                    return map;
                })
                .toList();
    }

    private Map<String, Object> getTrackSingleValueStats() {
        Map<String, Object> map = new HashMap<>();

        int totalTracksPlayed = playedTrackData.size();
        int totalUniqueTracksPlayed = distinctTrackData.size();
        long totalListeningTime = trackData.stream()
                .mapToLong(Track::durationMs)
                .sum();
        Set<Track> newPlayedTracks = new HashSet<>(trackData);
        newPlayedTracks.removeAll(excludedPlayedTrackData.stream()
                .map(PlayedTrack::track)
                .collect(Collectors.toSet()));
        int totalNewTracksPlayed = newPlayedTracks.size();
        long totalLocalTracksPlayed = trackData.stream()
                .filter(Track::isLocal)
                .count();
        long totalExplicitTracksPlayed = trackData.stream()
                .filter(Track::isExplicit)
                .count();
        long averageTrackDuration = Math.round(trackData.stream()
                .mapToLong(Track::durationMs)
                .average()
                .orElse(-1));
        double averageTrackPopularity = playedTrackData.stream()
                .filter(this::filterRecentTrack)
                .mapToInt(PlayedTrack::currentPopularity)
                .average()
                .orElse(-1);

        map.put("total_tracks_played", totalTracksPlayed);
        map.put("total_unique_tracks_played", totalUniqueTracksPlayed);
        map.put("total_listening_time", totalListeningTime);
        map.put("total_new_tracks_played", totalNewTracksPlayed);
        map.put("total_local_tracks_played", totalLocalTracksPlayed);
        map.put("total_explicit_tracks_played", totalExplicitTracksPlayed);
        map.put("average_track_duration", averageTrackDuration);
        map.put("average_track_popularity", averageTrackPopularity);

        return map;
    }


    boolean filterRecentTrack(PlayedTrack track) {
        LocalDate release = track.track().album().releaseDate();
        LocalDate threshold = LocalDate.now().minusDays(MINIMUM_DAYS_SINCE_RELEASE_POPULARITY);
        return !release.isAfter(threshold);
    }

    private Map<String, Object> getAlbumSingleValueStats() {
        Map<String, Object> map = new HashMap<>();

        int totalUniqueAlbumsPlayed = distinctAlbumData.size();

        map.put("total_unique_albums_played", totalUniqueAlbumsPlayed);

        return map;
    }

    private Map<String, Object> getArtistSingleValueStats() {
        Map<String, Object> map = new HashMap<>();

        int totalUniqueArtistsPlayed = distinctArtistData.size();

        map.put("total_unique_artists_played", totalUniqueArtistsPlayed);

        return map;
    }

    private Map<String, Object> getMiscSingleValueStats() {
        Map<String, Object> map = new HashMap<>();

        List<Session> sessions = calculateSessions();

        int totalListeningSessions = sessions.size();
        long averageListeningSessionLength = Math.round(sessions.stream()
                .mapToLong(x -> x.period().getDurationMs())
                .average()
                .orElse(-1));
        double averagePercentageListeningToMusicDuringSession = sessions.stream()
                .mapToDouble(
                        x -> (double) x.playedTracks().stream()
                                .mapToLong(y -> y.track().durationMs())
                                .sum() / x.period().getDurationMs())
                .average()
                .orElse(-1);
        int listeningStreak = getListeningStreakInDays();

        Set<Track> nonLikedSongs = playedTrackData.stream()
                .filter(x -> x.contextType().equals("playlist") || x.contextType().equals("album"))
                .map(PlayedTrack::track)
                .collect(Collectors.toSet());
        Set<Track> likedSongs = playedTrackData.stream()
                .filter(x -> x.contextType().equals("collection"))
                .map(PlayedTrack::track)
                .collect(Collectors.toSet());
        long nonLikedAddedCount = nonLikedSongs.stream()
                .filter(likedSongs::contains)
                .count();
        double percentageOfSongsAddedToLikedAfterPlay = (double) nonLikedAddedCount / nonLikedSongs.size();

        Set<Track> allLikedSongs = allPlayedTrackData.stream()
                .filter(x -> x.contextType().equals("collection"))
                .map(PlayedTrack::track)
                .collect(Collectors.toSet());
        long totalNewTracksAddedToLiked = likedSongs.stream()
                .filter(allLikedSongs::contains)
                .count();

        map.put("total_listening_sessions", totalListeningSessions);
        map.put("average_listening_session_length", averageListeningSessionLength);
        map.put("average_percentage_listening_to_music_during_session", averagePercentageListeningToMusicDuringSession);
        map.put("current_listening_streak", listeningStreak);
        map.put("percentage_tracks_added_to_liked_after_play", percentageOfSongsAddedToLikedAfterPlay);
        map.put("total_new_tracks_added_to_liked", totalNewTracksAddedToLiked);

        return map;
    }

    public Map<String, Object> getAllSingleValueStats() {
        Map<String, Object> map = new HashMap<>();

        map.put("track", getTrackSingleValueStats());
        map.put("album", getAlbumSingleValueStats());
        map.put("artists", getArtistSingleValueStats());
        map.put("misc", getMiscSingleValueStats());

        return map;
    }

    private List<Session> calculateSessions() {
        if (playedTrackData.isEmpty()) return Collections.emptyList();

        List<PlayedTrack> playedTracksSorted = new ArrayList<>(playedTrackData);
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
                                        Math.round(startingTrack.track().durationMs() * 0.65)
                                ),
                            endingTrack.timeFinished().plusMillis(
                                        Math.round(endingTrack.track().durationMs() * 0.25)
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

    public Map<String, Object> getLongestTrack() {
        return distinctTrackData.stream()
                .max(Comparator.comparingLong(Track::durationMs))
                .map(Track::toMap)
                .orElse(null);
    }

    public Map<String, Object> getShortestTrack() {
        return distinctTrackData.stream()
                .min(Comparator.comparingLong(Track::durationMs))
                .map(Track::toMap)
                .orElse(null);
    }

    private int getListeningStreakInDays() {
        ZoneId aus = ZoneId.of("Australia/Sydney");
        LocalDate checkingDate = LocalDate.now();

        boolean dayFound = false;
        int streak = 0;

        do {
            LocalDate dateForLambda = checkingDate;
            dayFound = allPlayedTrackData.stream()
                    .anyMatch(x -> {
                        LocalDate playedDate = x.timeFinished().atZone(aus).toLocalDate();
                        return playedDate.isEqual(dateForLambda);
                    });
            checkingDate = checkingDate.minusDays(1);
            if (dayFound) streak++;
        } while (dayFound);

        return streak;
    }

    public Map<String, Object> getLongestListeningSession() {
        return calculateSessions().stream()
                .max(Comparator.comparingLong(x -> x.period().getDurationMs()))
                .map(Session::toMap)
                .orElse(null);

    }

    private List<LocalTimeRange> generateTimeBlocks() {
        List<LocalTimeRange> timeBlocks = new ArrayList<>();
        for (int i = 1; i < 1 + (60 / TIME_BLOCK_LENGTH_MINUTES) * 24; i++) {
            LocalTime startTime = LocalTime.MIDNIGHT.plusMinutes(TIME_BLOCK_LENGTH_MINUTES * (i - 1));
            LocalTime endTime = LocalTime.MIDNIGHT.plusMinutes(TIME_BLOCK_LENGTH_MINUTES * i);
            timeBlocks.add(new LocalTimeRange(startTime, endTime));
        }
        return timeBlocks;
    }

    public Map<String, Object> getListeningTimeHeatMap() {
        List<LocalTimeRange> timeBlocks = generateTimeBlocks();
        List<Map<String, Object>> heatMap = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        map.put("heat_map", heatMap);
        ZoneId aus = ZoneId.of("Australia/Sydney");

        for (LocalTimeRange timeBlock : timeBlocks) {
            var timeBlockMap = timeBlock.toMap();
            List<PlayedTrack> timeBlockPlayedTracks = playedTrackData.stream()
                    .filter(x -> timeBlock.isWithin(LocalTime.ofInstant(x.timeFinished(), aus)))
                    .toList();
            long playCount = timeBlockPlayedTracks.size();
            long durationTotal = timeBlockPlayedTracks.stream().mapToLong(x -> x.track().durationMs()).sum();
            timeBlockMap.put("play_count", playCount);
            timeBlockMap.put("duration_total", durationTotal);
            heatMap.add(timeBlockMap);
        }

        map.put("min_play_count", 0);
        long maxPlayCount = heatMap.stream()
                .mapToLong(x -> (long) x.get("play_count"))
                .max()
                .orElse(-1);
        map.put("max_play_count", maxPlayCount);
        map.put("min_duration_total", 0);
        long maxDurationTotal = heatMap.stream()
                .mapToLong(x -> (long) x.get("duration_total"))
                .max()
                .orElse(-1);
        map.put("max_duration_total", maxDurationTotal);

        return map;
    }

    public List<Map<String, Object>> getArtistDistribution() {
        long totalPlays = playedTrackData.size();
        return distinctArtistData.stream()
                .map( artist -> {
                    Map<String, Object> map = artist.toMap();
                    long playCount = playedTrackData.stream()
                            .filter(pt -> pt.track().artists().contains(artist))
                            .count();
                    map.put("play_count", playCount);
                    map.put("percentage_of_tracks", (double) playCount /  totalPlays);
                    return map;
                })
                .sorted(Comparator.comparingDouble(x -> (double) x.get("percentage_of_tracks")))
                .toList()
                .reversed();
    }

    public Map<String, Object> getMostNicheArtist() {
        List<Artist> filteredArtists = artistData.stream()
                .filter(a -> !a.id().equals(EXCLUDE_ARTIST))
                .toList();
        int minPop = filteredArtists.stream()
                .mapToInt(Artist::popularity)
                .min()
                .orElse(-1);

        return filteredArtists.stream()
                .filter(a -> a.popularity() == minPop)
                .min(Comparator.comparingInt(Artist::followers))
                .map(Artist::toMap)
                .orElse(null);
    }

    public Map<String, Object> getMostPopularArtist() {
        List<Artist> filteredArtists = artistData.stream()
                .filter(a -> !a.id().equals(EXCLUDE_ARTIST))
                .toList();
        int maxPop = filteredArtists.stream()
                .mapToInt(Artist::popularity)
                .max()
                .orElse(-1);

        return filteredArtists.stream()
                .filter(a -> a.popularity() == maxPop)
                .max(Comparator.comparingInt(Artist::followers))
                .map(Artist::toMap)
                .orElse(null);
    }

    public List<Map<String, Object>> getRecentlyPlayedTracks(Integer limit) {
        return playedTrackData.stream()
                .limit(limit)
                .map(PlayedTrack::toMap)
                .toList();
    }

    public Map<String, Object> getMostNicheTrack() {
        return playedTrackData.stream()
                .filter(this::filterRecentTrack)
                .min(Comparator.comparingInt(PlayedTrack::currentPopularity))
                .map(PlayedTrack::toMap)
                .orElse(null);
    }


}
