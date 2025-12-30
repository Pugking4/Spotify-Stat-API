package com.[REDACTED].spotifystat.api.stats;

import com.[REDACTED].spotifystat.api.data.TrackRepository;
import com.[REDACTED].spotifystat.common.dto.PlayedTrack;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
class StatsController {

    private final TrackRepository trackRepository;

    public StatsController(TrackRepository trackRepository) {
        this.trackRepository = trackRepository;
    }

    @RequestMapping("/")
    public Map<String, Object> home() {
        Map<String, Object> info = new HashMap<>();
        info.put("message", "Welcome to Spotify Tracker API, this API retrieves various stats from my listening history.");
        info.put("version", "0.1.0");

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
            ☩ most common exact date of release

        collection:
            ☩ context type distribution
            ☩ added to liked stats

        sub categories for specific item based stats:
         */


        //info.put("endpoints", endpoints);
        return info;

        /*
        All-Time specific
        ☩ greatest rise in pop (exclude tracks at pop 0)

         */
    }

    @GetMapping("/stats/time/calendar/today")
    public Map<String, Object> today() {
        return getTimeStats(Calendar.THIS_DAY);
    }

    @GetMapping("/stats/time/calendar/day")
    public Map<String, Object> day(@RequestParam(required = false) Integer daysBack) {
        return getTimeStats(Calendar.DAY);
    }

    @GetMapping("/stats/time/calendar/this-week")
    public Map<String, Object> thisWeek() {
        return getTimeStats(Calendar.THIS_WEEK);
    }

    @GetMapping("/stats/time/calendar/week")
    public Map<String, Object> week(@RequestParam(required = false) Integer weeksBack) {
        return getTimeStats(Calendar.WEEK);
    }

    @GetMapping("/stats/time/calendar/this-month")
    public Map<String, Object> thisMonth() {
        return getTimeStats(Calendar.THIS_MONTH);
    }

    @GetMapping("/stats/time/calendar/month")
    public Map<String, Object> month(@RequestParam(required = false) Integer monthsBack) {
        return getTimeStats(Calendar.MONTH);
    }

    @GetMapping("/stats/time/calendar/this-year")
    public Map<String, Object> thisYear() {
        return getTimeStats(Calendar.THIS_YEAR);
    }

    @GetMapping("/stats/time/calendar/year")
    public Map<String, Object> year(@RequestParam(required = false) Integer yearsBack) {
        return getTimeStats(Calendar.YEAR);
    }

    @GetMapping("/stats/time/rolling")
    public ResponseEntity<Map<String, Object>> rolling(@RequestParam Integer endHours, @RequestParam(required = false) Integer startHours) {
        if (endHours < 0) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        var stats = getTimeStats(endHours);
        return new ResponseEntity<>(stats, HttpStatus.OK);
    }

    private Map<String, Object> getTimeStats(Calendar period) {

        return getTimeStats(trackRepository.findByPeriod(period));
    }

    private Map<String, Object> getTimeStats(int hours) {
        return getTimeStats(trackRepository.findByHours(hours));
    }

    private Map<String, Object> getTimeStats(List<PlayedTrack> playedTracks) {
        List<PlayedTrack> allTimeData = trackRepository.findAll();
        StatsService service = new StatsService(playedTracks, allTimeData);
        Map<String, Object> stats = new HashMap<>();
        stats.put("top_tracks", service.getTopFiveTracks());
        stats.put("single_value_stats", service.getAllSingleValueStats());
        stats.put("longest_track", service.getLongestTrack());
        stats.put("shortest_track", service.getShortestTrack());
        stats.put("longest_listening_session", service.getLongestListeningSession());
        stats.put("listening_time_heatmap", service.getListeningTimeHeatMap());
        stats.put("artist_distribution", service.getArtistDistribution());
        stats.put("most_niche_artist", service.getMostNicheArtist());
        stats.put("most_popular_artist", service.getMostPopularArtist());
        return stats;
    }

    @GetMapping("/stats/recentlyPlayed")
    public ResponseEntity<List<Map<String, Object>>> recentlyPlayed(@RequestParam Integer limit) {
        if (limit < 0) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        return new ResponseEntity<>(trackRepository.getRecentlyPlayedTracks(limit).stream()
                .map(PlayedTrack::toMap)
                .toList(), HttpStatus.OK);
    }
}
