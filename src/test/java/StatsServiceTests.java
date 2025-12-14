import com.[REDACTED].spotifystat.api.stats.StatsService;
import com.[REDACTED].spotifystat.common.dto.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class StatsServiceTests {
    private List<Artist> createNormalArtistData() {
        List<Artist> artists = new ArrayList<>();

        artists.add(new Artist("artist1", "Artist One", 100, List.of("rock"), "https://image1", 50, Instant.now()));
        artists.add(new Artist("artist2", "Artist Two", 200, List.of("pop"), "https://image2", 65, Instant.now()));
        artists.add(new Artist("artist3", "Artist Three", 300, List.of("jazz", "pop"), "https://image3", 60, Instant.now()));

        return artists;
    }

    private List<Album> createNormalAlbumData() {
        var artists = createNormalArtistData();

        Artist artist1 = artists.get(0);
        Artist artist2 = artists.get(1);
        Artist artist3 = artists.get(2);

        return List.of(
                new Album("album1", "Album 1", "https://album1", LocalDate.now().minusDays(3), "day", "album", List.of(artist1, artist2)),
                new Album("album2", "Single 2", "https://album2", LocalDate.now().minusDays(250), "day", "single", List.of(artist3)),
                new Album("album3", "Single 3", "https://album3", LocalDate.now().minusDays(1000), "day", "single", List.of(artist1))
        );
    }

    private List<Track> createNormalTrackData() {
        var albums = createNormalAlbumData();
        var artists = createNormalArtistData();
        Artist artist1 = artists.get(0);
        Artist artist2 = artists.get(1);
        Artist artist3 = artists.get(2);
        Album album1 = albums.get(0);
        Album album2 = albums.get(1);
        Album album3 = albums.get(2);

        return List.of(
                new Track("track1", "Track One", album1, 120000, false, false, List.of(artist1, artist2)),
                new Track("track2", "Track Two", album1, 180000, true, false, List.of(artist1, artist2)),
                new Track("track3", "Track Three", album1, 240000, false, true, List.of(artist2)),
                new Track("track4", "Track Four", album2, 300000, true, true, List.of(artist3)),
                new Track("track5", "Track Five", album1, 360000, false, false, List.of(artist1)),
                new Track("track6", "Track Six", album3, 340000, true, false, List.of(artist1))
        );
    }

    private List<Device> createNormalDeviceData() {
        return List.of(
                new Device("Device One", "Computer"),
                new Device("Device Two", "Smartphone")
        );
    }

    private List<PlayedTrack> createNormalTestingData(Instant baseline) {
        var tracks = createNormalTrackData();
        var devices = createNormalDeviceData();

        List<PlayedTrack> testData = new ArrayList<>();

        Device device1 = devices.get(0);
        Device device2 = devices.get(1);

        int seconds = 1;
        int minutes = seconds * 60;
        int hours = minutes * 60;
        int days = hours * 24;

        testData.add(new PlayedTrack(tracks.get(0), "collection", device1, 50, baseline.minusSeconds(30)));
        testData.add(new PlayedTrack(tracks.get(1), "collection", device2, 60, baseline.minusSeconds(90)));
        testData.add(new PlayedTrack(tracks.get(2), "playlist", device1, 70, baseline.minusSeconds(hours)));
        testData.add(new PlayedTrack(tracks.get(3), "playlist", device2, 80, baseline.minusSeconds(5 * hours)));
        testData.add(new PlayedTrack(tracks.get(4), "collection", device1, 90, baseline.minusSeconds(days)));
        testData.add(new PlayedTrack(tracks.get(0), "collection", device2, 50, baseline.minusSeconds(2 * days)));
        testData.add(new PlayedTrack(tracks.get(1), "playlist", device1, 60, baseline.minusSeconds(7 * days)));
        testData.add(new PlayedTrack(tracks.get(2), "collection", device2, 70, baseline.minusSeconds(14 * days)));
        testData.add(new PlayedTrack(tracks.get(3), "playlist", device1, 80, baseline.minusSeconds(30 * days)));
        testData.add(new PlayedTrack(tracks.get(4), "collection", device2, 90, baseline.minusSeconds(60 * days)));
        testData.add(new PlayedTrack(tracks.get(0), "collection", device1, 50, baseline.minusSeconds(90 * days)));
        testData.add(new PlayedTrack(tracks.get(1), "playlist", device2, 60, baseline.minusSeconds(180 * days)));
        testData.add(new PlayedTrack(tracks.get(2), "collection", device1, 70, baseline.minusSeconds(365 * days)));
        testData.add(new PlayedTrack(tracks.get(3), "playlist", device2, 80, baseline.minusSeconds(730 * days)));
        testData.add(new PlayedTrack(tracks.get(4), "collection", device1, 90, baseline.minusSeconds(1095 * days)));
        testData.add(new PlayedTrack(tracks.get(4), "collection", device1, 90, baseline.minusSeconds(1096 * days)));

        return testData;
    }

    @Test
    public void returns_top_five_tracks_under_normal_data_state() {
        List<PlayedTrack> testData = createNormalTestingData(Instant.now());
        StatsService statsService = new StatsService(testData, testData);
        List<Map<String, Object>> results = statsService.getTopFiveTracks();

        var trackData = createNormalTrackData();

        assertEquals(5, results.size());
        assertEquals(trackData.get(4).id(), results.get(0).get("id"));
        assertTrue(results.stream().anyMatch(track -> track.get("id").equals(trackData.get(1).id())));
        assertTrue(results.stream().anyMatch(track -> track.get("id").equals(trackData.get(2).id())));
        assertTrue(results.stream().anyMatch(track -> track.get("id").equals(trackData.get(3).id())));
        assertTrue(results.stream().anyMatch(track -> track.get("id").equals(trackData.get(0).id())));
        assertFalse(results.stream().anyMatch(track -> track.get("id").equals(trackData.get(5).id())));
    }

    @Test
    public void returns_empty_top_five_tracks_under_empty_data_state() {
        List<PlayedTrack> testData = createNormalTestingData(Instant.now());
        StatsService statsService = new StatsService(List.of(), testData);
        List<Map<String, Object>> results = statsService.getTopFiveTracks();

        assertEquals(0, results.size());
    }

    private List<PlayedTrack> createLargeTestingData() {
        List<PlayedTrack> testData = new ArrayList<>();
        var tracks = createNormalTrackData();
        var devices = createNormalDeviceData();
        Instant now = Instant.now();

        for (int i = 0; i < 40000; i++) {
            Track track = tracks.get(0);
            Device device = devices.get(0);
            Instant time = now.minusSeconds(i * 9);
            int popularity = 50 + (i % 50);

            testData.add(new PlayedTrack(track, "collection", device, popularity, time));
        }

        for (int i = 0; i < 7500; i++) {
            Track track = tracks.get(1);
            Device device = devices.get(0);
            Instant time = now.minusSeconds(i * 873);
            int popularity = 50 + (i % 50);

            testData.add(new PlayedTrack(track, "playlist", device, popularity, time));
        }

        for (int i = 0; i < 200; i++) {
            Track track = tracks.get(2);
            Device device = devices.get(1);
            Instant time = now.minusSeconds(i * 171);
            int popularity = 50 + (i % 50);

            testData.add(new PlayedTrack(track, "album", device, popularity, time));
        }
        for (int i = 0; i < 2300; i++) {
            Track track = tracks.get(3);
            Device device = devices.get(0);
            Instant time = now.minusSeconds(i * 37);
            int popularity = 50 + (i % 50);

            testData.add(new PlayedTrack(track, "collection", device, popularity, time));
        }

        for (int i = 0; i < 5000; i++) {
            Track track = tracks.get(4);
            Device device = devices.get(0);
            Instant time = now.minusSeconds(i * 777);
            int popularity = 50 + (i % 50);

            testData.add(new PlayedTrack(track, "collection", device, popularity, time));
        }

        for (int i = 0; i < 2; i++) {
            Track track = tracks.get(5);
            Device device = devices.get(0);
            Instant time = now.minusSeconds(i * 547);
            int popularity = 50 + (i % 50);

            testData.add(new PlayedTrack(track, "playlist", device, popularity, time));
        }

        return testData;
    }


    @Test
    public void returns_top_five_tracks_under_very_large_data_state() {
        List<PlayedTrack> testData = createLargeTestingData();
        var tracks = createNormalTrackData();

        StatsService statsService = new StatsService(testData, testData);
        List<Map<String, Object>> results = statsService.getTopFiveTracks();

        assertEquals(5, results.size());
        assertEquals(tracks.get(0).id(), results.get(0).get("id"));
        assertEquals(tracks.get(1).id(), results.get(1).get("id"));
        assertEquals(tracks.get(4).id(), results.get(2).get("id"));
        assertEquals(tracks.get(3).id(), results.get(3).get("id"));
        assertEquals(tracks.get(2).id(), results.get(4).get("id"));

        assertFalse(results.stream().anyMatch(track -> track.get("id").equals(tracks.get(5).id())));
    }

    @Test
    public void returns_empty_top_five_tracks_under_very_large_data_state() {
        List<PlayedTrack> testData = createLargeTestingData();
        var tracks = createNormalTrackData();

        StatsService statsService = new StatsService(List.of(), testData);
        List<Map<String, Object>> results = statsService.getTopFiveTracks();

        assertEquals(0, results.size());
    }
}
