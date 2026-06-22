package com.flipradar.api;

import com.flipradar.data.LocalCache;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public final class HypixelApiClient {
    private static final String AUCTIONS_URL = "https://api.hypixel.net/v2/skyblock/auctions?page=";

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final LocalCache cache;

    public HypixelApiClient(LocalCache cache) {
        this.cache = cache;
    }

    public Optional<JsonObject> getAuctionPage(int page) {
        String cacheKey = "auction-page-" + page + ".json";
        Optional<String> cached = cache.readFresh(cacheKey, Duration.ofSeconds(60));
        if (cached.isPresent()) {
            return Optional.of(JsonParser.parseString(cached.get()).getAsJsonObject());
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(AUCTIONS_URL + page))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "FlipRadar/0.1.0 manual informational SkyBlock mod")
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                cache.write(cacheKey, response.body(), Instant.now());
                return Optional.of(JsonParser.parseString(response.body()).getAsJsonObject());
            }
            if (response.statusCode() == 429) {
                return cache.readAny(cacheKey).map(body -> JsonParser.parseString(body).getAsJsonObject());
            }
        } catch (IOException | InterruptedException ignored) {
            if (ignored instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }

        return cache.readAny(cacheKey).map(body -> JsonParser.parseString(body).getAsJsonObject());
    }
}
