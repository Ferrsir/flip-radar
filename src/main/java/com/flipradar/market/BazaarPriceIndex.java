package com.flipradar.market;

import com.flipradar.api.HypixelApiClient;
import com.google.gson.JsonObject;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class BazaarPriceIndex {
    private final HypixelApiClient apiClient;
    private final Map<String, Long> prices = new ConcurrentHashMap<>();
    private Instant lastRefresh = Instant.EPOCH;

    public BazaarPriceIndex(HypixelApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void refreshIfStale() {
        if (lastRefresh.plus(Duration.ofMinutes(5)).isAfter(Instant.now())) {
            return;
        }

        Optional<JsonObject> bazaar = apiClient.getBazaar();
        if (bazaar.isEmpty() || !bazaar.get().has("products")) {
            return;
        }

        JsonObject products = bazaar.get().getAsJsonObject("products");
        for (Map.Entry<String, com.google.gson.JsonElement> entry : products.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject product = entry.getValue().getAsJsonObject();
            if (!product.has("quick_status")) {
                continue;
            }
            JsonObject quickStatus = product.getAsJsonObject("quick_status");
            double buyPrice = quickStatus.has("buyPrice") ? quickStatus.get("buyPrice").getAsDouble() : 0.0D;
            double sellPrice = quickStatus.has("sellPrice") ? quickStatus.get("sellPrice").getAsDouble() : 0.0D;
            double conservativePrice = Math.max(buyPrice, sellPrice);
            if (conservativePrice > 0.0D) {
                prices.put(entry.getKey(), Math.round(conservativePrice));
            }
        }
        lastRefresh = Instant.now();
    }

    public long price(String productId) {
        refreshIfStale();
        return prices.getOrDefault(productId, 0L);
    }
}
