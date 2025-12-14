package com.[REDACTED].spotifystat.api.stats;

import com.[REDACTED].spotifystat.common.dto.InstantTimeRange;
import com.[REDACTED].spotifystat.common.dto.PlayedTrack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record Session(InstantTimeRange period, List<PlayedTrack> playedTracks) {
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("time_range", period.toMap());

        List<Map<String, Object>> convertedPlayedTracks = new ArrayList<>();
        map.put("played_tracks", convertedPlayedTracks);
        for (PlayedTrack playedTrack : playedTracks) {
            convertedPlayedTracks.add(playedTrack.toMap());
        }
        return map;
    }
}
