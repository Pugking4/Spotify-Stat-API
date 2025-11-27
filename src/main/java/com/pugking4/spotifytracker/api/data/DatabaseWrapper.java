package com.[REDACTED].spotifytracker.api.data;
import java.sql.*;

import com.[REDACTED].spotifytracker.api.stats.Calendar;
import com.[REDACTED].spotifytracker.dto.Album;
import com.[REDACTED].spotifytracker.dto.Artist;
import com.[REDACTED].spotifytracker.dto.Track;
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

    private static Pair<LocalDateTime, LocalDateTime> getCutoffForMode(Calendar mode) {
        return switch(mode) {
            case DAILY -> new Pair<>(LocalDateTime.now(), LocalDate.now().atStartOfDay());
            case WEEKLY -> new Pair<>(LocalDateTime.now(), LocalDate.now().with(java.time.DayOfWeek.MONDAY).atStartOfDay());
            case MONTHLY -> new Pair<>(LocalDateTime.now(), LocalDate.now().withDayOfMonth(1).atStartOfDay());
            case YEARLY -> new Pair<>(LocalDateTime.now(), LocalDate.now().withDayOfYear(1).atStartOfDay());
            case YESTERDAY -> new Pair<>(LocalDate.now().atStartOfDay(), LocalDate.now().minusDays(1).atStartOfDay());
            case LAST_WEEK -> new Pair<>(LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay(), LocalDate.now().minusWeeks(1).with(java.time.DayOfWeek.MONDAY).atStartOfDay());
            case LAST_MONTH -> new Pair<>(LocalDate.now().withDayOfMonth(1).atStartOfDay(), LocalDate.now().minusMonths(1).withDayOfMonth(1).atStartOfDay());
            case LAST_YEAR -> new Pair<>(LocalDate.now().withDayOfYear(1).atStartOfDay(), LocalDate.now().minusYears(1).withDayOfYear(1).atStartOfDay());
        };
    }

    private static Pair<LocalDateTime, LocalDateTime> getCutoffForHours(int hours) {
        return new Pair<>(LocalDateTime.now(), LocalDateTime.now().minusHours(hours));
    }

    public static List<Map<String, Object>> getTopTracks(Calendar mode) {
        return getTopTracks(getCutoffForMode(mode));
    }

    public static List<Map<String, Object>> getTopTracks(int hours) {
        return getTopTracks(getCutoffForHours(hours));
    }

    private static List<Map<String, Object>> getTopTracks(Pair<LocalDateTime, LocalDateTime> trackingPeriod) {
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
