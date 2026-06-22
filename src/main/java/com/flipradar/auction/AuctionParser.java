package com.flipradar.auction;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public final class AuctionParser {
    private final ItemSignatureBuilder signatureBuilder = new ItemSignatureBuilder();

    public Optional<AuctionItem> parse(JsonObject auction) {
        if (!auction.has("bin") || !auction.get("bin").getAsBoolean()) {
            return Optional.empty();
        }

        String name = stringValue(auction, "item_name", "Unknown Item");
        String lore = stringValue(auction, "item_lore", "");
        String itemBytes = "";
        JsonObject bytes = auction.has("item_bytes") && auction.get("item_bytes").isJsonObject()
                ? auction.getAsJsonObject("item_bytes")
                : null;
        if (bytes != null && bytes.has("data")) {
            itemBytes = bytes.get("data").getAsString();
        }

        String uuidText = stringValue(auction, "uuid", UUID.randomUUID().toString());
        String seller = stringValue(auction, "auctioneer", "unknown");
        String tier = stringValue(auction, "tier", "UNKNOWN");
        long price = longValue(auction, "starting_bid", 0L);
        Instant start = Instant.ofEpochMilli(longValue(auction, "start", System.currentTimeMillis()));
        Instant end = Instant.ofEpochMilli(longValue(auction, "end", System.currentTimeMillis()));
        String signature = signatureBuilder.build(name, lore, tier, itemBytes);

        return Optional.of(new AuctionItem(parseUuid(uuidText), seller, name, lore, itemBytes, tier, price, start, end, signature));
    }

    private String stringValue(JsonObject object, String key, String fallback) {
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsString();
    }

    private long longValue(JsonObject object, String key, long fallback) {
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsLong();
    }

    private UUID parseUuid(String value) {
        try {
            if (value.length() == 32) {
                return UUID.fromString(value.replaceFirst(
                        "([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{12})",
                        "$1-$2-$3-$4-$5"
                ));
            }
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return UUID.randomUUID();
        }
    }
}
