package com.flipradar.auction;

import java.time.Instant;
import java.util.UUID;

public record AuctionItem(
        UUID uuid,
        String seller,
        String itemName,
        String itemLore,
        String itemBytes,
        String tier,
        long binPrice,
        Instant start,
        Instant end,
        String signature
) {
}
