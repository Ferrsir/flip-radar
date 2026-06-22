package com.flipradar.auction;

import com.flipradar.api.HypixelApiClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AuctionPageFetcher {
    private final HypixelApiClient apiClient;
    private final AuctionParser parser = new AuctionParser();

    public AuctionPageFetcher(HypixelApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public List<AuctionItem> fetchBinAuctions(int pageLimit) {
        List<AuctionItem> auctions = new ArrayList<>();
        int pages = Math.max(1, pageLimit);

        Optional<JsonObject> first = apiClient.getAuctionPage(0);
        if (first.isEmpty()) {
            return auctions;
        }

        pages = Math.min(pages, first.get().has("totalPages") ? first.get().get("totalPages").getAsInt() : pages);
        parseInto(auctions, first.get());

        for (int page = 1; page < pages; page++) {
            apiClient.getAuctionPage(page).ifPresent(json -> parseInto(auctions, json));
        }

        return auctions;
    }

    private void parseInto(List<AuctionItem> auctions, JsonObject page) {
        JsonArray array = page.getAsJsonArray("auctions");
        if (array == null) {
            return;
        }
        array.forEach(element -> parser.parse(element.getAsJsonObject()).ifPresent(auctions::add));
    }
}
