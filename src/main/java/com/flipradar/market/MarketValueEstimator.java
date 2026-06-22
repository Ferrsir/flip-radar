package com.flipradar.market;

import com.flipradar.auction.AuctionItem;

import java.util.Comparator;
import java.util.List;

public final class MarketValueEstimator {
    public long estimate(AuctionItem auction, List<AuctionItem> activeAuctions) {
        List<Long> comparablePrices = activeAuctions.stream()
                .filter(candidate -> candidate.signature().equals(auction.signature()))
                .map(AuctionItem::binPrice)
                .sorted()
                .toList();

        if (comparablePrices.size() < 4) {
            return 0L;
        }

        int trimmedStart = Math.max(0, comparablePrices.size() / 10);
        int trimmedEnd = Math.max(trimmedStart + 1, comparablePrices.size() - trimmedStart);
        List<Long> trimmed = comparablePrices.subList(trimmedStart, trimmedEnd);
        return median(trimmed);
    }

    private long median(List<Long> values) {
        return values.stream()
                .sorted(Comparator.naturalOrder())
                .skip((values.size() - 1L) / 2L)
                .findFirst()
                .orElse(0L);
    }
}
