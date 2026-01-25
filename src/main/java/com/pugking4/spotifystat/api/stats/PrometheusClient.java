package com.pugking4.spotifystat.api.stats;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

@Component
public class PrometheusClient {
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper;
    private final URI baseUri = URI.create("http://localhost:9090");

    public PrometheusClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode instantQuery(String promQl, Instant time) {
        try {
            String url = baseUri + "/api/v1/query"
                    + "?query=" + URLEncoder.encode(promQl, StandardCharsets.UTF_8)
                    + "&time=" + time.getEpochSecond();

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> resp =
                    httpClient.send(req, HttpResponse.BodyHandlers.ofString());

            JsonNode root = objectMapper.readTree(resp.body());

            if (!"success".equals(root.path("status").asText())) {
                throw new IllegalStateException(
                        "Prometheus error: " + root.path("error").asText());
            }

            return root.path("data").path("result");
        } catch (Exception e) {
            throw new IllegalStateException("Prometheus query failed: " + promQl, e);
        }
    }
}
