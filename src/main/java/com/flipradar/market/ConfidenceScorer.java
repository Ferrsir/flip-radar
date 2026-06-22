package com.flipradar.market;

import com.flipradar.auction.AuctionItem;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public final class ConfidenceScorer {
    public int score(AuctionItem auction, long estimatedMarketValue, double profitPercent, List<AuctionItem> activeAuctions) {
        long comparableCount = activeAuctions.stream()
                .filter(candidate -> candidate.signature().equals(auction.signature()))
                .count();

        int score = 30;
        score += Math.min(30, comparableCount * 3);
        score += Math.min(20, (int) Math.round(profitPercent));
        score += estimatedMarketValue > auction.binPrice() ? 10 : -20;

        long ageMinutes = Duration.between(auction.start(), Instant.now()).toMinutes();
        if (ageMinutes > 120) {
            score -= 15;
        }

        if (auction.signature().contains("attributes") || auction.signature().contains("gemstones")) {
            score -= 8;
        }

        return Math.max(0, Math.min(100, score));
    }
}
