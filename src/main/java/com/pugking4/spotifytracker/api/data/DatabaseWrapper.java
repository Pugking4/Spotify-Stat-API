package com.[REDACTED].spotifytracker.api.data;
import java.sql.*;

import com.[REDACTED].spotifytracker.api.stats.Calendar;
import com.[REDACTED].spotifytracker.dto.*;
import com.[REDACTED].spotifytracker.api.data.Pair;

import java.time.*;
import java.util.*;

public class DatabaseWrapper {
    private final static Connection db;
    private final static String url = "jdbc:postgresql://localhost:5433/track-database";
    private final static String username = "[REDACTED]";
    private final static String [REDACTED];

    static {
        try {
            Properties props = new Properties();
            props.setProperty("user", username);
            props.setProperty("password", password);
            db = DriverManager.getConnection(url, props);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Map<String, Object>> getTopTracks(Pair<LocalDateTime, LocalDateTime> trackingPeriod) {
        List<Map<String, Object>> topTracks = new ArrayList<>();

        try {
            String sql = """
                    SELECT t.id, t.name, t.album_id, t.duration_ms, t.is_explicit, t.is_local, a.name as album_name, a.cover, a.release_date, a.release_date_precision, a.album_type, COUNT(th.track_id)
                    FROM track_history AS th
                             JOIN tracks AS t ON th.track_id = t.id
                             JOIN albums AS a ON th.album_id = a.id
                    WHERE th.time_finished > ? AND th.time_finished < ?
                    GROUP BY t.id, t.name, t.album_id, t.duration_ms, t.is_explicit, t.is_local, a.name, a.cover, a.release_date, a.release_date_precision, a.album_type
                    ORDER BY COUNT(th.track_id) DESC
                    LIMIT 5;
                """;


            PreparedStatement st = db.prepareStatement(sql);
            st.setTimestamp(2, Timestamp.valueOf(trackingPeriod.left()));
            st.setTimestamp(1, Timestamp.valueOf(trackingPeriod.right()));
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                Map<String, Object> trackData = new HashMap<>();
                trackData.put("id", rs.getString("id"));
                trackData.put("name", rs.getString("name"));
                trackData.put("album_id", rs.getString("album_id"));
                trackData.put("duration_ms", rs.getInt("duration_ms"));
                trackData.put("is_explicit", rs.getBoolean("is_explicit"));
                trackData.put("is_local", rs.getBoolean("is_local"));

                Map<String, Object> albumData = new HashMap<>();
                albumData.put("name", rs.getString("album_name"));
                albumData.put("cover", rs.getString("cover"));
                albumData.put("release_date", rs.getTimestamp("release_date").toLocalDateTime().toLocalDate().toString());
                albumData.put("release_date_precision", rs.getString("release_date_precision"));
                albumData.put("album_type", rs.getString("album_type"));
                trackData.put("album", albumData);

                trackData.put("play_count", rs.getInt("count"));

                // Add artists list as needed, e.g., trackData.put("artists", getTrackArtists(rs.getString("id")));

                topTracks.add(trackData);
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return topTracks;
    }

    private static List<Map<String, Object>> getAlbumArtists(String albumID) {
        try {
            String sql = """
                    SELECT a.id, a.name
                    FROM (
                        SELECT artist_id
                        FROM album_artist
                        WHERE album_id = ?
                    ) AS aa
                        JOIN artists AS a ON aa.artist_id = a.id;
                """;

            PreparedStatement st = db.prepareStatement(sql);
            st.setString(1, albumID);
            return getCombinationArtists(st);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<Map<String, Object>> getTrackArtists(String trackID) {
        try {
            String sql = """
                    SELECT a.id, a.name
                    FROM (
                        SELECT artist_id
                        FROM track_artist
                        WHERE track_id = ?
                    ) AS ta
                        JOIN artists AS a ON ta.artist_id = a.id;
                """;

            PreparedStatement st = db.prepareStatement(sql);
            st.setString(1, trackID);
            return getCombinationArtists(st);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<Map<String, Object>> getCombinationArtists(PreparedStatement ps) {
        List<Map<String, Object>> artists = new ArrayList<>();
        try {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> artist = new HashMap<>();
                artist.put("id", rs.getString("id"));
                artist.put("name", rs.getString("name"));
                artists.add(artist);
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return artists;
    }

    public static int getAverageSongDuration(Pair<LocalDateTime, LocalDateTime> trackingPeriod) {
        int avgDuration = -1;

        try {
            String sql = """
                    SELECT AVG(t.duration)
                    FROM track_history AS th
                             JOIN tracks AS t ON th.track_id = t.id
                    WHERE th.time_finished > ? AND th.time_finished < ?;
                """;


            PreparedStatement st = db.prepareStatement(sql);
            st.setTimestamp(2, Timestamp.valueOf(trackingPeriod.left()));
            st.setTimestamp(1, Timestamp.valueOf(trackingPeriod.right()));
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                avgDuration = rs.getInt("duration_ms");
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return avgDuration;
    }

    public static List<PlayedTrack> collectAllData(Pair<LocalDateTime, LocalDateTime> trackingPeriod) {
        List<PlayedTrack> playedTracks = new ArrayList<>();

        try {
            String sql = """
                    SELECT th.*, t.name as track_name, t.duration_ms, t.is_explicit, t.is_local, a.name as album_name, a.cover, a.release_date, a.release_date_precision, a.album_type, d.*
                    FROM track_history AS th
                        JOIN tracks AS t ON th.track_id = t.id
                        JOIN albums AS a ON th.album_id = a.id
                        JOIN devices as d ON th.device_name = d.name
                    WHERE th.time_finished > ? AND th.time_finished < ?;
                """;


            PreparedStatement st = db.prepareStatement(sql);
            st.setTimestamp(1, Timestamp.valueOf(trackingPeriod.right()));
            st.setTimestamp(2, Timestamp.valueOf(trackingPeriod.left()));

            collectAllData2(playedTracks, st);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return playedTracks;
    }

    private static void collectAllData2(List<PlayedTrack> playedTracks, PreparedStatement st) throws SQLException {
        ResultSet rs = st.executeQuery();

        while (rs.next()) {
            String contextType = rs.getString("context_type");
            String albumId = rs.getString("album_id");
            String trackId = rs.getString("track_id");
            String deviceName = rs.getString("device_name");
            Integer currentPopularity = rs.getInt("current_popularity");
            Instant timePlayed = rs.getTimestamp("time_finished").toInstant();

            // Extract track data
            String trackName = rs.getString("track_name");
            Integer durationMs = rs.getInt("duration_ms");
            Boolean isExplicit = rs.getBoolean("is_explicit");
            Boolean isLocal = rs.getBoolean("is_local");

            // Extract album data
            String albumName = rs.getString("album_name");
            String cover = rs.getString("cover");
            LocalDate releaseDate = rs.getTimestamp("release_date").toLocalDateTime().toLocalDate();
            String releaseDatePrecision = rs.getString("release_date_precision");
            String albumType = rs.getString("album_type");

            // Extract device data
            String deviceType = rs.getString("type");

            // Get album artists
            List<Artist> albumArtists = getAlbumArtists(albumId).stream()
                    .map(map -> new Artist((String) map.get("id"), (String) map.get("name")))
                    .toList();

            // Get track artists
            List<Artist> trackArtists = getTrackArtists(trackId).stream()
                    .map(map -> new Artist((String) map.get("id"), (String) map.get("name")))
                    .toList();

            // Build Album record
            Album album = new Album(albumId, albumName, cover, releaseDate, releaseDatePrecision, albumType, albumArtists);

            // Build Track record
            Track track = new Track(trackId, trackName, album, durationMs, isExplicit, isLocal, trackArtists);

            // Build Device record
            Device device = new Device(deviceName, deviceType);

            // Build PlayedTrack record
            PlayedTrack playedTrack = new PlayedTrack(track, contextType, device, currentPopularity, timePlayed);
            playedTracks.add(playedTrack);
        }
        rs.close();
        st.close();
    }

    public static List<PlayedTrack> collectAllData() {
        List<PlayedTrack> playedTracks = new ArrayList<>();

        try {
            String sql = """
                    SELECT th.*, t.name as track_name, t.duration_ms, t.is_explicit, t.is_local, a.name as album_name, a.cover, a.release_date, a.release_date_precision, a.album_type, d.*
                    FROM track_history AS th
                        JOIN tracks AS t ON th.track_id = t.id
                        JOIN albums AS a ON th.album_id = a.id
                        JOIN devices as d ON th.device_name = d.name;
                """;


            PreparedStatement st = db.prepareStatement(sql);
            collectAllData2(playedTracks, st);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return playedTracks;
    }


}
