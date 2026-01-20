package com.pugking4.spotifystat.api.stats;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;
import org.springframework.boot.info.BuildProperties;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Service
public class APIStatsService {
    final MeterRegistry registry;
    private final BuildProperties build;

    public APIStatsService(MeterRegistry registry, BuildProperties build) {
        this.registry = registry;
        this.build = build;
    }

    public double getUptimeSeconds() {
        var g = registry.find("process.uptime").gauge();
        return (g != null) ? g.value() : -1;
    }

    public long getTotalRequests() {
        return registry.find("http.server.requests")
                .timers().stream()
                .mapToLong(Timer::count)
                .sum();
    }

    public String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }

    public String getVersion() {
        return build.getVersion();
    }
}
