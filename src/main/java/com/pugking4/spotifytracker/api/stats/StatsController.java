package com.pugking4.spotifytracker.api.stats;

import com.pugking4.spotifytracker.api.data.DatabaseWrapper;
import com.pugking4.spotifytracker.api.data.Pair;
import com.pugking4.spotifytracker.dto.PlayedTrack;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
class StatsController {

    @RequestMapping("/")
    public Map<String, Object> home() {
        Map<String, Object> info = new HashMap<>();
        info.put("message", "Welcome to Spotify Tracker API, this API retrieves various stats from my listening history.");
        info.put("version", "1.0");

        /*String[] endpoints = new String[] {"/stats/api", "/stats/time/rolling?hours=",
                "/stats/time/calendar/complete/daily", "/stats/time/calendar/complete/weekly", "/stats/time/calendar/complete/monthly",
                "/stats/time/calendar/complete/yearly", "/stats/time/calendar/incomplete/daily", "/stats/time/calendar/incomplete/weekly", "/stats/time/calendar/incomplete/monthly",
                "/stats/time/calendar/incomplete/yearly", "/stats/time/all-time", "/stats/artist?name=", "/stats/genre?name=",
                "/stats/album?name=", "/stats/track?name=?artist=", "/stats/devices?name=", "/stats/context?type="};*/
        /*
        api stats:
            running time
            version
            queries served over lifetime
            queries served during this runtime


        sub categories for time based stats:

        tracks:
            ○ most played tracks (different than distribution, contains cover) (top 5)
            ○ totals (for everything, ms, # tracks, artists,
            !☩ genre distribution (% with # in subtext)
            !☩ most niche genre (if supported)
            !☩ most popular genre (if supported)
            ○ avg song length
            ○ longest song length
            ○ shortest song length
            ○ total minutes/hours played per day, week, month, year (depends on calendar mode/rolling length)
            ○ most active listening time (hotspots on 24hr format)
            ○ current streak of listening (in days)
            ○ longest listening session (without 15 min break)
            !○ Most Listened Time Blocks (top 10, include more info about top one above)
            ○ average session length
            !☩ most common genres for each timeframe (calculate some kind of genre consistency, the highest the more consistent the genres the lower the less and more varied, choose highest for each time period)
            ○ how often new songs are played from liked collection and how many (in one day, played for first time in liked collection NOT PLAYLIST)
            !☩ top days of week, top week of months, top month of years (depends on calendar mode/rolling length)
            !☩ track distribution (might not be great, dont really listen on repeat very often)
            ☩ get most niche track (atleast 1 month old as new tracks are 0 pop by default)
            ☩ get most popular track


        artists:
            ○ artist distribution (% with # in subtext)
            ☩ most niche artist (if supported)
            ☩ most popular artist (if supported)
            ☩ most common artist pairing (must be >= 2)

        devices:
            ☩ device distribution
            ☩ most common time of day for each device (hotspots on 24hr format) (combine with tracks variation, like an overlay?)

        albums:
            ☩ album distribution (maybe?)
            ☩ most common era of release date
            ☩ most common year of release date
            ☩ distribution of album type

        collection:
            ☩ context type distribution
            ☩ added to liked stats

        sub categories for specific item based stats:
         */


        //info.put("endpoints", endpoints);
        return info;
    }

    @GetMapping("/stats/time/calendar/today")
    public Map<String, Object> today() {
        return getTimeStats(getTrackingPeriod(Calendar.THIS_DAY));
    }

    @GetMapping("/stats/time/calendar/day")
    public Map<String, Object> day(@RequestParam(required = false) Integer daysBack) {
        return getTimeStats(getTrackingPeriod(Calendar.DAY));
    }

    @GetMapping("/stats/time/calendar/this-week")
    public Map<String, Object> thisWeek() {
        return getTimeStats(getTrackingPeriod(Calendar.THIS_WEEK));
    }

    @GetMapping("/stats/time/calendar/week")
    public Map<String, Object> week(@RequestParam(required = false) Integer weeksBack) {
        return getTimeStats(getTrackingPeriod(Calendar.WEEK));
    }

    @GetMapping("/stats/time/calendar/this-month")
    public Map<String, Object> thisMonth() {
        return getTimeStats(getTrackingPeriod(Calendar.THIS_MONTH));
    }

    @GetMapping("/stats/time/calendar/month")
    public Map<String, Object> month(@RequestParam(required = false) Integer monthsBack) {
        return getTimeStats(getTrackingPeriod(Calendar.MONTH));
    }

    @GetMapping("/stats/time/calendar/this-year")
    public Map<String, Object> thisYear() {
        return getTimeStats(getTrackingPeriod(Calendar.THIS_YEAR));
    }

    @GetMapping("/stats/time/calendar/year")
    public Map<String, Object> year(@RequestParam(required = false) Integer yearsBack) {
        return getTimeStats(getTrackingPeriod(Calendar.YEAR));
    }

    @GetMapping("/stats/time/rolling")
    public ResponseEntity<Map<String, Object>> rolling(@RequestParam Integer endHours, @RequestParam(required = false) Integer startHours) {
        if (endHours < 0) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        var stats = getTimeStats(getTrackingPeriod(endHours));
        return new ResponseEntity<>(stats, HttpStatus.OK);
    }

    private static Map<String, Object> getTimeStats(Pair<LocalDateTime, LocalDateTime> trackingPeriod) {
        List<PlayedTrack> trackData = DatabaseWrapper.collectAllData(trackingPeriod);
        StatsService service = new StatsService(trackData);
        Map<String, Object> stats = new HashMap<>();
        stats.put("top_tracks", service.getTopTracks());
        stats.put("single_value_stats", service.getSingleValueStats());
        stats.put("longest_track", service.getLongestTrack());
        stats.put("shortest_track", service.getShortestTrack());
        stats.put("longest_listening_session", service.getLongestListeningSession());
        stats.put("listening_time_heatmap", service.getListeningTimeHeatMap());
        stats.put("artist_distribution", service.getArtistDistribution());
        stats.put("most_niche_artist", service.getMostNicheArtist());
        stats.put("most_popular_artist", service.getMostPopularArtist());
        return stats;
    }

    private static Pair<LocalDateTime, LocalDateTime> getTrackingPeriod(Calendar mode) {
        return switch(mode) {
            case THIS_DAY -> new Pair<>(LocalDateTime.now(), LocalDate.now().atStartOfDay());
            case THIS_WEEK -> new Pair<>(LocalDateTime.now(), LocalDate.now().with(java.time.DayOfWeek.MONDAY).atStartOfDay());
            case THIS_MONTH -> new Pair<>(LocalDateTime.now(), LocalDate.now().withDayOfMonth(1).atStartOfDay());
            case THIS_YEAR -> new Pair<>(LocalDateTime.now(), LocalDate.now().withDayOfYear(1).atStartOfDay());
            case DAY -> new Pair<>(LocalDate.now().atStartOfDay(), LocalDate.now().minusDays(1).atStartOfDay());
            case WEEK -> new Pair<>(LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay(), LocalDate.now().minusWeeks(1).with(java.time.DayOfWeek.MONDAY).atStartOfDay());
            case MONTH -> new Pair<>(LocalDate.now().withDayOfMonth(1).atStartOfDay(), LocalDate.now().minusMonths(1).withDayOfMonth(1).atStartOfDay());
            case YEAR -> new Pair<>(LocalDate.now().withDayOfYear(1).atStartOfDay(), LocalDate.now().minusYears(1).withDayOfYear(1).atStartOfDay());
        };
    }

    private static Pair<LocalDateTime, LocalDateTime> getTrackingPeriod(int hours) {
        return new Pair<>(LocalDateTime.now(), LocalDateTime.now().minusHours(hours));
    }

}
