package com.pugking4.spotifystat.api.stats;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class APIStatsService {
    final MeterRegistry registry;
    final BuildProperties build;
    private final PrometheusClient prom;

    public APIStatsService(MeterRegistry registry, BuildProperties build, PrometheusClient prom) {
        this.registry = registry;
        this.build = build;
        this.prom = prom;
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

    private String requestsPromQl(String rangeLiteral) {
        return "sum by (outcome) (increase(http_server_requests_seconds_count{job=\"spring-boot-app\"}[" + rangeLiteral + "]))"
                + " or label_replace(vector(0), \"outcome\", \"SUCCESS\", \"\", \"\")"
                + " or label_replace(vector(0), \"outcome\", \"CLIENT_ERROR\", \"\", \"\")"
                + " or label_replace(vector(0), \"outcome\", \"SERVER_ERROR\", \"\", \"\")";
    }

    public RequestsPerMinuteResponse getRequestsPerMinutes(int minutes) {
        Instant now = Instant.now();
        Instant prevEnd = TimeBuckets.alignEnd(now, minutes);
        Instant nextEnd = prevEnd.plus(Duration.ofMinutes(minutes));

        long partialSec = TimeBuckets.clampRange(Duration.between(prevEnd, now).getSeconds());
        Map<String, Double> complete = parseOutcome(prom.instantQuery(requestsPromQl(minutes + "m"), prevEnd));
        Map<String, Double> partial = parseOutcome(prom.instantQuery(requestsPromQl(partialSec + "s"), now));

        return new RequestsPerMinuteResponse(
                minutes,
                List.of(
                        new RequestBucket(
                                prevEnd.getEpochSecond(),
                                true,
                                complete.get("SUCCESS"),
                                complete.get("CLIENT_ERROR"),
                                complete.get("SERVER_ERROR")
                        ),
                        new RequestBucket(
                                nextEnd.getEpochSecond(),
                                false,
                                partial.get("SUCCESS"),
                                partial.get("CLIENT_ERROR"),
                                partial.get("SERVER_ERROR")
                        )
                )
        );
    }

    private String latencyQuantilesPromQl(String selector, String rangeLiteral) {
        String bucketsRate =
                "sum(rate(http_server_requests_seconds_bucket{" + selector + "}[" + rangeLiteral + "])) by (le)";

        return "label_replace(histogram_quantile(0.50, " + bucketsRate + "), \"quantile\", \"0.50\", \"\", \"\")\n"
                + "or label_replace(histogram_quantile(0.95, " + bucketsRate + "), \"quantile\", \"0.95\", \"\", \"\")\n"
                + "or label_replace(histogram_quantile(0.99, " + bucketsRate + "), \"quantile\", \"0.99\", \"\", \"\")";
    }

    public LatencyResponse getLatencyPercentilesMs(String uri, int windowMinutes) {
        Instant now = Instant.now();
        Instant prevEnd = TimeBuckets.alignEnd(now, windowMinutes);
        Instant nextEnd = prevEnd.plus(Duration.ofMinutes(windowMinutes));

        String selector = (uri == null || uri.isBlank())
                ? "job=\"spring-boot-app\""
                : "job=\"spring-boot-app\",uri=\"" + uri + "\"";

        long partialSec = TimeBuckets.clampRange(Duration.between(prevEnd, now).getSeconds());

        Map<String, Double> complete =
                parseQuantilesMs(
                        prom.instantQuery(
                                latencyQuantilesPromQl(selector, windowMinutes + "m"),
                                prevEnd
                        )
                );

        Map<String, Double> partial =
                parseQuantilesMs(
                        prom.instantQuery(
                                latencyQuantilesPromQl(selector, partialSec + "s"),
                                now
                        )
                );

        return new LatencyResponse(
                windowMinutes,
                List.of(
                        new LatencyBucket(
                                prevEnd.getEpochSecond(),
                                true,
                                windowMinutes * 60L,
                                complete.get("p50_ms"),
                                complete.get("p95_ms"),
                                complete.get("p99_ms")
                        ),
                        new LatencyBucket(
                                nextEnd.getEpochSecond(),
                                false,
                                partialSec,
                                partial.get("p50_ms"),
                                partial.get("p95_ms"),
                                partial.get("p99_ms")
                        )
                )
        );
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

    public Map<String, Double> getUptimePercentage() {
        Map<String, Integer> frames = Map.of(
                "d1", 1,
                "d7", 7
        );

        Map<String, Double> out = new HashMap<>();
        Instant now = Instant.now();

        for (var entry : frames.entrySet()) {
            String label = entry.getKey();
            int days = entry.getValue();

            String promQl = String.format(
                    "avg_over_time(up{job=\"spring-boot-app\"}[%dd]) * 100",
                    days
            );

            double pct =
                    parseSingleValuePct(
                            prom.instantQuery(promQl, now)
                    );

            out.put(label, pct);
        }

        return out;
    }


    private Map<String, Double> parseOutcome(JsonNode result) {
        Map<String, Double> out = new HashMap<>();

        for (JsonNode ts : result) {
            String key = ts.path("metric").path("outcome").asText("unknown");
            double value = ts.path("value").get(1).asDouble();
            out.put(key, value);
        }

        out.putIfAbsent("SUCCESS", 0.0);
        out.putIfAbsent("CLIENT_ERROR", 0.0);
        out.putIfAbsent("SERVER_ERROR", 0.0);
        return out;
    }

    private Map<String, Double> parseQuantilesMs(JsonNode result) {
        Map<String, Double> out = new HashMap<>();

        for (JsonNode ts : result) {
            String q = ts.path("metric").path("quantile").asText();
            double ms = ts.path("value").get(1).asDouble() * 1000.0;

            switch (q) {
                case "0.50" -> out.put("p50_ms", ms);
                case "0.95" -> out.put("p95_ms", ms);
                case "0.99" -> out.put("p99_ms", ms);
            }
        }

        out.putIfAbsent("p50_ms", Double.NaN);
        out.putIfAbsent("p95_ms", Double.NaN);
        out.putIfAbsent("p99_ms", Double.NaN);
        return out;
    }

    private double parseSingleValuePct(JsonNode result) {
        if (!result.isArray() || result.isEmpty()) {
            return 0.0;
        }
        return result.get(0).path("value").get(1).asDouble();
    }


}

