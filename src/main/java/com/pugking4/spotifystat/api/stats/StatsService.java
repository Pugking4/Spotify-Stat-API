package com.pugking4.spotifystat.api.stats;

import com.pugking4.spotifystat.api.data.DatabaseWrapper;
import com.pugking4.spotifystat.common.dto.*;

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
    private final String EXCLUDE_ARTIST = "0LyfQWJT6nXafLPZqxe9Of";

    private final int MAX_LISTENING_SESSION_GAP_MINUTES = 15;
    private final int TIME_BLOCK_LENGTH_MINUTES = 15; // must be a factor of 60

    public StatsService(List<PlayedTrack> playedTrackData) {
        this.playedTrackData = playedTrackData;
        this.allPlayedTrackData = DatabaseWrapper.collectAllData();
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

    public List<Map<String, Object>> getTopTracks() {
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

    public Map<String, Object> getSingleValueStats() {
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> trackMap = new HashMap<>();
        map.put("track", trackMap);

        int totalTracksPlayed = playedTrackData.size();
        int totalUniqueTracksPlayed = distinctTrackData.size();
        long totalListeningTime = trackData.stream()
                .mapToLong(Track::durationMs)
                .sum();
        Set<Track> newPlayedTracks = new HashSet<>(trackData);
        newPlayedTracks.removeAll(excludedPlayedTrackData.stream().map(PlayedTrack::track).collect(Collectors.toSet()));
        int totalNewTracksPlayed = newPlayedTracks.size();
        long totalLocalTracksPlayed = trackData.stream()
                .filter(Track::isLocal)
                .count();
        long totalExplicitTracksPlayed = trackData.stream()
                .filter(Track::isExplicit)
                .count();
        long averageTrackDuration = Math.round(trackData.stream().mapToLong(Track::durationMs).average().orElse(-1));
        double averageTrackPopularity = playedTrackData.stream().mapToInt(PlayedTrack::currentPopularity).average().orElse(-1);

        trackMap.put("total_tracks_played", totalTracksPlayed);
        trackMap.put("total_unique_tracks_played", totalUniqueTracksPlayed);
        trackMap.put("total_listening_time", totalListeningTime);
        trackMap.put("total_new_tracks_played", totalNewTracksPlayed);
        trackMap.put("total_local_tracks_played", totalLocalTracksPlayed);
        trackMap.put("total_explicit_tracks_played", totalExplicitTracksPlayed);
        trackMap.put("average_track_duration", averageTrackDuration);
        trackMap.put("average_track_popularity", averageTrackPopularity);

        Map<String, Object> albumMap = new HashMap<>();
        map.put("album", albumMap);

        int totalUniqueAlbumsPlayed = distinctAlbumData.size();

        albumMap.put("total_unique_albums_played", totalUniqueAlbumsPlayed);

        Map<String, Object> artistMap = new HashMap<>();
        map.put("artists", artistMap);

        int totalUniqueArtistsPlayed = distinctArtistData.size();

        artistMap.put("total_unique_artists_played", totalUniqueArtistsPlayed);

        Map<String, Object> miscMap = new HashMap<>();
        map.put("misc", miscMap);

        List<Session> sessions = getSessions();

        int totalListeningSessions = sessions.size();
        long averageListeningSessionLength = Math.round(sessions.stream().mapToLong(x -> x.end().toEpochMilli() - x.start().toEpochMilli()).average().orElse(-1));
        double averagePercentageListeningToMusicDuringSession = sessions.stream()
                .mapToDouble(
                        x -> (double) x.playedTracks().stream()
                                .mapToLong(y -> y.track().durationMs())
                                .sum() / (x.end().toEpochMilli() - x.start().toEpochMilli()))
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

        miscMap.put("total_listening_sessions", totalListeningSessions);
        miscMap.put("average_listening_session_length", averageListeningSessionLength);
        miscMap.put("average_percentage_listening_to_music_during_session", averagePercentageListeningToMusicDuringSession);
        miscMap.put("current_listening_streak", listeningStreak);
        miscMap.put("percentage_tracks_added_to_liked_after_play", percentageOfSongsAddedToLikedAfterPlay);
        miscMap.put("total_new_tracks_added_to_liked", totalNewTracksAddedToLiked);

        return map;
    }

    private List<Session> getSessions() {
        if (playedTrackData.isEmpty()) return Collections.emptyList();
        List<Session> sessions = new ArrayList<>();
        List<PlayedTrack> currentSessionPlayedTracks = new ArrayList<>();
        Instant previousSong = playedTrackData.getFirst().timeFinished();

        for (PlayedTrack playedTrack : playedTrackData) {
            Instant currentSong = playedTrack.timeFinished();
            if (currentSong.isAfter(previousSong.plusSeconds(MAX_LISTENING_SESSION_GAP_MINUTES * 60))) {
                if (!currentSessionPlayedTracks.isEmpty()) {
                    sessions.add(new Session(
                            currentSessionPlayedTracks.getFirst().timeFinished().minusMillis(
                                    currentSessionPlayedTracks.getFirst().track().durationMs()
                            ),
                            currentSessionPlayedTracks.getLast().timeFinished().plusMillis(
                                    Math.round(currentSessionPlayedTracks.getLast().track().durationMs() * 0.35)
                            ),
                            new ArrayList<>(currentSessionPlayedTracks)));
                    currentSessionPlayedTracks.clear();
                }
            }
            currentSessionPlayedTracks.add(playedTrack);
            previousSong = currentSong;
        }

        if (!currentSessionPlayedTracks.isEmpty()) {
            sessions.add(new Session(
                    currentSessionPlayedTracks.getFirst().timeFinished(),
                    currentSessionPlayedTracks.getLast().timeFinished(),
                    currentSessionPlayedTracks
            ));
        }

        /*for (Session session : sessions) {
            String message = "Total session length: " + (session.end().toEpochMilli() - session.start().toEpochMilli());
            for (PlayedTrack playedTrack : session.playedTracks()) {
                message += ", " + playedTrack.track().name() + " - " + playedTrack.track().durationMs();
            }
            System.out.println(message);
        }*/

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
        return getSessions().stream()
                .max(Comparator.comparingLong(x -> x.end().toEpochMilli() - x.start().toEpochMilli()))
                .map(Session::toMap)
                .orElse(null);

    }

    private List<TimeRange> generateTimeBlocks() {
        List<TimeRange> timeBlocks = new ArrayList<>();
        for (int i = 1; i < 1 + (60 / TIME_BLOCK_LENGTH_MINUTES) * 24; i++) {
            LocalTime startTime = LocalTime.MIDNIGHT.plusMinutes(TIME_BLOCK_LENGTH_MINUTES * (i - 1));
            LocalTime endTime = LocalTime.MIDNIGHT.plusMinutes(TIME_BLOCK_LENGTH_MINUTES * i);
            timeBlocks.add(new TimeRange(startTime, endTime));
        }
        return timeBlocks;
    }

    public Map<String, Object> getListeningTimeHeatMap() {
        List<TimeRange> timeBlocks = generateTimeBlocks();
        List<Map<String, Object>> heatMap = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        map.put("heat_map", heatMap);
        ZoneId aus = ZoneId.of("Australia/Sydney");

        for (TimeRange timeBlock : timeBlocks) {
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
                    map.put("percentage_of_tracks", playCount /  totalPlays);
                    return map;
                })
                .toList();
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
}
