package com.pugking4.spotifystat.api.data;
import java.sql.*;

import com.pugking4.spotifystat.api.stats.Calendar;
import com.pugking4.spotifystat.api.stats.TimeUtility;
import com.pugking4.spotifystat.common.dto.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.*;
import java.util.*;

@Repository
public class TrackRepository {
    private Connection db;
    private final static String url = "jdbc:postgresql://localhost:5433/track-database";
    @Value("${database.username}")
    private String username;
    @Value("${database.password}")
    private String password;

    @PostConstruct
    private void init() {
        if (username == null || password == null) {
            throw new IllegalStateException("Database credentials not configured");
        }
        try {
            Properties props = new Properties();
            props.setProperty("user", username);
            props.setProperty("password", password);
            db = DriverManager.getConnection(url, props);
        } catch (SQLException e) {
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    private List<Artist> getAlbumArtists(String albumID) {
        try {
            String sql = """
                    SELECT a.id, a.name, a.followers, a.genres, a.image, a.popularity, a.updated_at
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

    private List<Artist> getTrackArtists(String trackID) {
        try {
            String sql = """
                    SELECT a.id, a.name, a.followers, a.genres, a.image, a.popularity, a.updated_at
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

    private List<Artist> getCombinationArtists(PreparedStatement ps) {
        List<Artist> artists = new ArrayList<>();
        try {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String id = rs.getString("id");
                String name = rs.getString("name");
                Integer followers = (Integer) rs.getObject("followers");
                String rawGenres = rs.getString("genres");
                List<String> genres = null;
                if (rawGenres != null && !rawGenres.isEmpty()) {
                    genres = List.of(rawGenres.split(","));
                }
                String image = rs.getString("image");
                Integer popularity = (Integer) rs.getObject("popularity");
                Timestamp timeStampAt = rs.getTimestamp("updated_at");
                Instant updatedAt = null;
                if (timeStampAt != null) {
                    updatedAt = timeStampAt.toInstant();
                }

                Artist artist = new Artist(id, name, followers, genres, image, popularity, updatedAt);
                artists.add(artist);
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return artists;
    }

    public List<PlayedTrack> getRecentlyPlayedTracks(Integer limit) {
        return findInPeriod(null, limit);
    }

    public List<PlayedTrack> findByPeriod(Calendar period) {
        Pair<LocalDateTime, LocalDateTime> trackingPeriod = TimeUtility.getTrackingPeriod(period);
        return findInPeriod(trackingPeriod);
    }

    public List<PlayedTrack> findByHours(int hours) {
        Pair<LocalDateTime, LocalDateTime> trackingPeriod = TimeUtility.getTrackingPeriod(hours);
        return findInPeriod(trackingPeriod);
    }

    public List<PlayedTrack> findAll() {
        return findInPeriod(null);
    }

    public List<PlayedTrack> findInPeriod(Pair<LocalDateTime, LocalDateTime> trackingPeriod) {
        try {
            PreparedStatement st;
            if (trackingPeriod == null) {
                String sql = """
                    SELECT th.*, t.name as track_name, t.duration_ms, t.is_explicit, t.is_local, a.name as album_name, a.cover, a.release_date, a.release_date_precision, a.album_type, d.*
                    FROM track_history AS th
                        JOIN tracks AS t ON th.track_id = t.id
                        JOIN albums AS a ON th.album_id = a.id
                        JOIN devices as d ON th.device_name = d.name
                    ORDER BY th.time_finished DESC;
                """;
                st = db.prepareStatement(sql);
            } else {
                String sql = """
                    SELECT th.*, t.name as track_name, t.duration_ms, t.is_explicit, t.is_local, a.name as album_name, a.cover, a.release_date, a.release_date_precision, a.album_type, d.*
                    FROM track_history AS th
                        JOIN tracks AS t ON th.track_id = t.id
                        JOIN albums AS a ON th.album_id = a.id
                        JOIN devices as d ON th.device_name = d.name
                    WHERE th.time_finished > ? AND th.time_finished < ?
                    ORDER BY th.time_finished DESC;
                """;
                st = db.prepareStatement(sql);
                st.setTimestamp(1, Timestamp.valueOf(trackingPeriod.right()));
                st.setTimestamp(2, Timestamp.valueOf(trackingPeriod.left()));
            }

            List<PlayedTrack> playedTracks = mapResultSetToPlayedTracks(st.executeQuery());
            st.close();
            return playedTracks;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<PlayedTrack> findInPeriod(Pair<LocalDateTime, LocalDateTime> trackingPeriod, Integer limit) {
        try {
            PreparedStatement st;
            if (trackingPeriod == null) {
                String sql = """
                    SELECT th.*, t.name as track_name, t.duration_ms, t.is_explicit, t.is_local, a.name as album_name, a.cover, a.release_date, a.release_date_precision, a.album_type, d.*
                    FROM track_history AS th
                        JOIN tracks AS t ON th.track_id = t.id
                        JOIN albums AS a ON th.album_id = a.id
                        JOIN devices as d ON th.device_name = d.name
                    ORDER BY th.time_finished DESC
                    LIMIT ?;
                """;
                st = db.prepareStatement(sql);
                st.setInt(1, limit);
            } else {
                String sql = """
                    SELECT th.*, t.name as track_name, t.duration_ms, t.is_explicit, t.is_local, a.name as album_name, a.cover, a.release_date, a.release_date_precision, a.album_type, d.*
                    FROM track_history AS th
                        JOIN tracks AS t ON th.track_id = t.id
                        JOIN albums AS a ON th.album_id = a.id
                        JOIN devices as d ON th.device_name = d.name
                    WHERE th.time_finished > ? AND th.time_finished < ?
                    ORDER BY th.time_finished DESC
                    LIMIT ?;
                """;
                st = db.prepareStatement(sql);
                st.setTimestamp(1, Timestamp.valueOf(trackingPeriod.right()));
                st.setTimestamp(2, Timestamp.valueOf(trackingPeriod.left()));
                st.setInt(3, limit);
            }

            List<PlayedTrack> playedTracks = mapResultSetToPlayedTracks(st.executeQuery());
            st.close();
            return playedTracks;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<PlayedTrack> mapResultSetToPlayedTracks(ResultSet rs) throws SQLException {
        List<PlayedTrack> playedTracks = new ArrayList<>();

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
            List<Artist> albumArtists = getAlbumArtists(albumId);

            // Get track artists
            List<Artist> trackArtists = getTrackArtists(trackId);

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
        return playedTracks;
    }
}
