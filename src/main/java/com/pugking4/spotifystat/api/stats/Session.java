package com.pugking4.spotifystat.api.stats;

import com.pugking4.spotifystat.common.dto.PlayedTrack;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record Session(Instant start, Instant end, List<PlayedTrack> playedTracks) {
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("start", start);
        map.put("end", end);

        List<Map<String, Object>> convertedPlayedTracks = new ArrayList<>();
        map.put("played_tracks", convertedPlayedTracks);
        for (PlayedTrack playedTrack : playedTracks) {
            convertedPlayedTracks.add(playedTrack.toMap());
        }
        return map;
    }
}
