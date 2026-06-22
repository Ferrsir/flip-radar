package com.flipradar.market;

import com.flipradar.auction.AuctionItem;

public record FlipCandidate(
        AuctionItem auction,
        long estimatedMarketValue,
        long estimatedAuctionTax,
        long profitAfterTax,
        double profitPercent,
        int confidencePercent,
        double volumePerDay,
        int ageMinutes
) {
}
