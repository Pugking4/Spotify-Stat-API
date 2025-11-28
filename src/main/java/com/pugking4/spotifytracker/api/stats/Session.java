package com.pugking4.spotifytracker.api.stats;

import com.pugking4.spotifytracker.dto.PlayedTrack;

import java.time.Instant;
import java.util.List;

public record Session(Instant start, Instant end, List<PlayedTrack> playedTracks) {
}
