package com.pugking4.spotifystat.api.stats;

import com.pugking4.spotifystat.api.data.TrackRepository;
import com.pugking4.spotifystat.common.dto.PlayedTrack;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
class StatsController {

    private final TrackRepository trackRepository;
    private final APIStatsService apiStatsService;

    public StatsController(TrackRepository trackRepository, APIStatsService apiStatsService) {
        this.trackRepository = trackRepository;
        this.apiStatsService = apiStatsService;
    }

    @RequestMapping("/")
    public Map<String, Object> home() {
        Map<String, Object> info = new HashMap<>();
        info.put("message", "Welcome to Spotify Tracker API, this API retrieves various stats from my listening history.");
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
            ☩ get most niche track (at least 1 month old as new tracks are 0 pop by default)
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
        ☩ the greatest rise in pop (exclude tracks at pop 0)

         */
    }

    @GetMapping("/stats/time")
    public ResponseEntity<TimePeriodStatsResponse> timeStats(@Valid TimeStatsRequest timeStatsRequest) {
        if (timeStatsRequest.mode() == TimeMode.CALENDAR) {
            return new ResponseEntity<>(getTimeStats(trackRepository.findByPeriod(timeStatsRequest.period(), timeStatsRequest.offset())), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(getTimeStats(trackRepository.findByHours(timeStatsRequest.hours(), timeStatsRequest.offset())), HttpStatus.OK);
        }
    }

    private TimePeriodStatsResponse getTimeStats(List<PlayedTrack> playedTracks) {
        List<PlayedTrack> allTimeData = trackRepository.findAll();
        StatsComputation service = new StatsComputation(playedTracks, allTimeData);

        return new TimePeriodStatsResponse(
                service.topTracks(5),
                service.calculateAllSingleValueStats(),
                service.findLongestTrack(),
                service.findShortestTrack(),
                service.findLongestListeningSession(),
                service.calculateListeningTimeHeatmap(),
                service.calculateArtistDistribution(),
                service.findMostNicheArtist(),
                service.findMostPopularArtist()
        );
    }

    @GetMapping("/stats/recently-played")
    public ResponseEntity<List<PlayedTrack>> recentlyPlayed(@RequestParam @Min(1) @NonNull Integer limit) {
        return new ResponseEntity<>(trackRepository.getRecentlyPlayedTracks(limit), HttpStatus.OK);
    }

    @GetMapping("/stats/api")
    public ApiStatsResponse api() {
        return new ApiStatsResponse(
                apiStatsService.getUptimeSeconds(),
                apiStatsService.getTotalRequests(),
                apiStatsService.getVersion(),
                apiStatsService.getHostName(),
                apiStatsService.getRequestsPerMinutes(1),
                apiStatsService.getRequestsPerMinutes(5),
                apiStatsService.getLatencyPercentilesMs("", 1),
                apiStatsService.getLatencyPercentilesMs("", 5),
                apiStatsService.getUptimePercentage()
        );
    }
}
