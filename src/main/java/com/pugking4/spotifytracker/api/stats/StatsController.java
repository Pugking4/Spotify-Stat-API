package com.pugking4.spotifytracker.api.stats;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
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

        String[] endpoints = new String[] {"/stats"};

        info.put("endpoints", endpoints);
        return info;
    }

}
