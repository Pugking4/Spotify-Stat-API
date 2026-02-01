package com.pugking4.spotifystat.api.stats;

import com.pugking4.spotifystat.common.dto.Track;

public record TrackPlayCount(Track track, int playCount) {
}
