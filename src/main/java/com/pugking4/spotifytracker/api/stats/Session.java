package com.[REDACTED].spotifytracker.api.stats;

import com.[REDACTED].spotifytracker.dto.PlayedTrack;

import java.time.Instant;
import java.util.List;

public record Session(Instant start, Instant end, List<PlayedTrack> playedTracks) {
}
