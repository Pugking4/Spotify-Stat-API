package com.[REDACTED].spotifytracker.api.stats;

import com.[REDACTED].spotifytracker.api.data.DatabaseWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
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

    @RequestMapping("/")
    public Map<String, Object> home() {
        Map<String, Object> info = new HashMap<>();
        info.put("message", "Welcome to Spotify Tracker API, this API retrieves various stats from my listening history.");
        info.put("version", "1.0");

        String[] endpoints = new String[] {"/stats/api", "/stats/time/rolling?hours=",
                "/stats/time/calendar/complete/daily", "/stats/time/calendar/complete/weekly", "/stats/time/calendar/complete/monthly",
                "/stats/time/calendar/complete/yearly", "/stats/time/calendar/incomplete/daily", "/stats/time/calendar/incomplete/weekly", "/stats/time/calendar/incomplete/monthly",
                "/stats/time/calendar/incomplete/yearly", "/stats/time/all-time", "/stats/artist?name=", "/stats/genre?name=",
                "/stats/album?name=", "/stats/track?name=?artist=", "/stats/devices?name=", "/stats/context?type="};
        /*
        api stats:
            running time
            version
            queries served over lifetime
            queries served during this runtime


        sub categories for time based stats:

        tracks:
            ☩ genre distribution (% with # in subtext)
            ☩ track distribution
            ☩ most niche genre (if supported)
            ☩ most popular genre (if supported)
            ○ most played tracks (different than distrobution, contains cover) (top 5)
            ☩ avg song length
            ☩ longest song length
            ☩ shortest song length
            ☩ total minutes/hours played per day, week, month, year (depends on calendar mode/rolling length)
            ☩ most active listening time (hotspots on 24hr format)
            ☩ current streak of listening (in days)
            ☩ longest listening session (without 15 min break)
            ☩ Most Listened Time Blocks (top 10, include more info about top one above)
            ☩ average session length
            ☩ most common genres for each timeframe (calculate some kind of genre consistency, the highest the more consistent the genres the lower the less and more varied, choose highest for each time period)
            ☩ how often new songs are played from liked collection and how many (in one day, played for first time in liked collection NOT PLAYLIST)
            ☩ top days of week, top week of months, top month of years (depends on calendar mode/rolling length)


        artists:
            ☩ artist distribution (% with # in subtext)
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

        sub categories for specific item based stats:
         */


        info.put("endpoints", endpoints);
        return info;
    }

    @GetMapping("/stats/time/calendar/incomplete/daily")
    public Map<String, Object> incompleteDaily() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("top_tracks", DatabaseWrapper.getTopTracks(Calendar.DAILY));
        return stats;
    }

    @GetMapping("/stats/time/calendar/complete/daily")
    public Map<String, Object> completeDaily() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("top_tracks", DatabaseWrapper.getTopTracks(Calendar.YESTERDAY));
        return stats;
    }

    @GetMapping("/stats/time/calendar/incomplete/weekly")
    public Map<String, Object> incompleteWeekly() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("top_tracks", DatabaseWrapper.getTopTracks(Calendar.WEEKLY));
        return stats;
    }

    @GetMapping("/stats/time/calendar/complete/weekly")
    public Map<String, Object> completeWeekly() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("top_tracks", DatabaseWrapper.getTopTracks(Calendar.LAST_WEEK));
        return stats;
    }

    @GetMapping("/stats/time/calendar/incomplete/yearly")
    public Map<String, Object> incompleteYearly() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("top_tracks", DatabaseWrapper.getTopTracks(Calendar.YEARLY));
        return stats;
    }

    @GetMapping("/stats/time/calendar/complete/yearly")
    public Map<String, Object> completeYearly() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("top_tracks", DatabaseWrapper.getTopTracks(Calendar.LAST_YEAR));
        return stats;
    }

    @GetMapping("/stats/time/rolling")
    public ResponseEntity<Map<String, Object>> rolling(@RequestParam int hours) {
        if (hours < 0) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        Map<String, Object> stats = new HashMap<>();
        stats.put("top_tracks", DatabaseWrapper.getTopTracks(hours));
        return new ResponseEntity<>(stats, HttpStatus.OK);
    }

}
