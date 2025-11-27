package com.pugking4.spotifytracker.api.data;
import java.sql.*;

import ch.qos.logback.core.joran.sanity.Pair;
import com.pugking4.spotifytracker.api.stats.Calendar;
import com.pugking4.spotifytracker.dto.Album;
import com.pugking4.spotifytracker.dto.Artist;
import com.pugking4.spotifytracker.dto.Track;

import java.time.*;
import java.util.*;

public class DatabaseWrapper {
    private final static Connection db;
    private final static String url = "jdbc:postgresql://localhost:5433/track-database";
    private final static String username = "pugking4";
    private final static String password = "apples";

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

    private static LocalDateTime getCutoffForMode(Calendar mode) {
        return switch(mode) {
            case DAILY -> LocalDate.now().atStartOfDay();
            case WEEKLY -> LocalDate.now().with(java.time.DayOfWeek.MONDAY).atStartOfDay();
            case MONTHLY -> LocalDate.now().withDayOfMonth(1).atStartOfDay();
            case YEARLY -> LocalDate.now().withDayOfYear(1).atStartOfDay();
            case YESTERDAY -> LocalDate.now().minusDays(1).atStartOfDay();
            case LAST_WEEK -> LocalDate.now().minusWeeks(1).with(java.time.DayOfWeek.MONDAY).atStartOfDay();
            case LAST_MONTH -> LocalDate.now().minusMonths(1).withDayOfMonth(1).atStartOfDay();
            case LAST_YEAR -> LocalDate.now().minusYears(1).withDayOfYear(1).atStartOfDay();
        };
    }

    private static LocalDateTime getCutoffForHours(int hours) {
        return LocalDateTime.now().minusHours(hours);
    }

    public static Map<Track, Integer> getTopTracks(Calendar mode) {
        return getTopTracks(getCutoffForMode(mode));
    }

    public static Map<Track, Integer> getTopTracks(int hours) {
        return getTopTracks(getCutoffForHours(hours));
    }

    private static Map<Track, Integer> getTopTracks(LocalDateTime cutoffPeriod) {
        Map<Track, Integer> topTracks = new HashMap<>();

        try {
            String sql = """
                    SELECT t.id, t.name, t.album_id, t.duration_ms, t.is_explicit, t.is_local, a.name as album_name, a.cover, a.release_date, a.release_date_precision, a.album_type, COUNT(th.track_id)
                    FROM track_history AS th
                             JOIN tracks AS t ON th.track_id = t.id
                             JOIN albums AS a ON th.album_id = a.id
                    WHERE th.time_finished > ?
                    GROUP BY t.id, t.name, t.album_id, t.duration_ms, t.is_explicit, t.is_local, a.name, a.cover, a.release_date, a.release_date_precision, a.album_type
                    ORDER BY COUNT(th.track_id) DESC
                    LIMIT 5;
                """;


            PreparedStatement st = db.prepareStatement(sql);
            st.setTimestamp(1, Timestamp.valueOf(cutoffPeriod));
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                System.out.println(rs.getString(1));
                String albumID = rs.getString("album_id");
                Album album = new Album(albumID,
                        rs.getString("album_name"),
                        rs.getString("cover"),
                        rs.getTimestamp("release_date").toLocalDateTime().toLocalDate(),
                        rs.getString("release_date_precision"),
                        rs.getString("album_type"),
                        getAlbumArtists(albumID));
                String trackID = rs.getString("id");
                Track track = new Track(trackID,
                        rs.getString("name"),
                        album,
                        rs.getInt("duration_ms"),
                        rs.getBoolean("is_explicit"),
                        rs.getBoolean("is_local"),
                        getTrackArtists(trackID));
                int count = rs.getInt("count");
                topTracks.put(track, count);
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return topTracks;
    }

    private static List<Artist> getAlbumArtists(String albumID) {
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

    private static List<Artist> getCombinationArtists(PreparedStatement ps) {
        List<Artist> artists = new ArrayList<>();
        try {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                System.out.println(rs.getString(1));
                Artist artist  = new Artist(rs.getString("id"), rs.getString("name"));
                artists.add(artist);
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return artists;
    }

    private static List<Artist> getTrackArtists(String trackID) {
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


}
