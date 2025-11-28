package com.pugking4.spotifytracker.api.stats;

import com.pugking4.spotifytracker.api.data.DatabaseWrapper;
import com.pugking4.spotifytracker.dto.*;

import java.time.Instant;
import java.time.LocalDateTime;
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

    private final int MAX_LISTENING_SESSION_GAP = 15;

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

    private static Map<String, Object> convertToMap(Album album) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", album.id());
        map.put("name", album.name());
        map.put("cover", album.cover());
        map.put("release_date", album.releaseDate() != null ? album.releaseDate().toString() : null);
        map.put("release_date_precision", album.releaseDatePrecision());
        map.put("album_type", album.type());
        map.put("artists", album.artists() != null ? album.artists().stream().map(StatsService::convertToMap).toList() : null);
        return map;
    }

    private static Map<String, Object> convertToMap(Artist artist) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", artist.id());
        map.put("name", artist.name());
        return map;
    }

    private static Map<String, Object> convertToMap(Device device) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", device.name());
        map.put("type", device.type());
        return map;
    }

    private static Map<String, Object> convertToMap(Track track) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", track.id());
        map.put("name", track.name());
        map.put("album", track.album() != null ? convertToMap(track.album()) : null);
        map.put("duration_ms", track.durationMs());
        map.put("is_explicit", track.isExplicit());
        map.put("is_local", track.isLocal());
        map.put("artists", track.artists() != null ? track.artists().stream().map(StatsService::convertToMap).toList() : null);
        return map;
    }

    private static Map<String, Object> convertToMap(PlayedTrack playedTrack) {
        Map<String, Object> map = new HashMap<>();
        map.put("track", playedTrack.track() != null ? convertToMap(playedTrack.track()) : null);
        map.put("context_type", playedTrack.contextType());
        map.put("device", playedTrack.device() != null ? convertToMap(playedTrack.device()) : null);
        map.put("current_popularity", playedTrack.currentPopularity());
        map.put("time_finished", playedTrack.time_played() != null ? playedTrack.time_played().toString() : null);
        return map;
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
                    Map<String, Object> map = convertToMap(entry.getKey());
                    map.put("play_count", entry.getValue());
                    return map;
                })
                .toList();
    }

    public long getAverageSongDuration() {
        OptionalDouble avgDuration = trackData.stream().mapToLong(Track::durationMs).average();
        return Math.round(avgDuration.orElse(-1));
    }

    public Map<String, Object> getTotals() {
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

        trackMap.put("total_tracks_played", totalTracksPlayed);
        trackMap.put("total_unique_tracks_played", totalUniqueTracksPlayed);
        trackMap.put("total_listening_time", totalListeningTime);
        trackMap.put("total_new_tracks_played", totalNewTracksPlayed);
        trackMap.put("total_local_tracks_played", totalLocalTracksPlayed);
        trackMap.put("total_explicit_tracks_played", totalExplicitTracksPlayed);

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

        miscMap.put("total_listening_sessions", totalListeningSessions);
        miscMap.put("average_listening_session_length", averageListeningSessionLength);
        miscMap.put("average_percentage_listening_to_music_during_session", averagePercentageListeningToMusicDuringSession);

        return map;
    }

    private List<Session> getSessions() {
        List<Session> sessions = new ArrayList<>();
        List<PlayedTrack> currentSessionPlayedTracks = new ArrayList<>();
        Instant previousSong = playedTrackData.getFirst().time_played();

        for (PlayedTrack playedTrack : playedTrackData) {
            Instant currentSong = playedTrack.time_played();
            if (currentSong.isAfter(previousSong.plusSeconds(MAX_LISTENING_SESSION_GAP * 60))) {
                if (!currentSessionPlayedTracks.isEmpty()) {
                    sessions.add(new Session(
                            currentSessionPlayedTracks.getFirst().time_played().minusMillis(
                                    currentSessionPlayedTracks.getFirst().track().durationMs()
                            ),
                            currentSessionPlayedTracks.getLast().time_played().plusMillis(
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
                    currentSessionPlayedTracks.getFirst().time_played(),
                    currentSessionPlayedTracks.getLast().time_played(),
                    currentSessionPlayedTracks
            ));
        }

        for (Session session : sessions) {
            String message = "Total session length: " + (session.end().toEpochMilli() - session.start().toEpochMilli());
            for (PlayedTrack playedTrack : session.playedTracks()) {
                message += ", " + playedTrack.track().name() + " - " + playedTrack.track().durationMs();
            }
            System.out.println(message);
        }

        return sessions;
    }
}
