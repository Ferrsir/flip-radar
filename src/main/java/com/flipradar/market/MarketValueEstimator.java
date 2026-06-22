package com.flipradar.market;

import com.flipradar.auction.AuctionItem;

import java.util.Comparator;
import java.util.List;

public final class MarketValueEstimator {
    public long estimate(AuctionItem auction, List<AuctionItem> activeAuctions) {
        List<Long> comparablePrices = activeAuctions.stream()
                .filter(candidate -> candidate.signature().equals(auction.signature()))
                .filter(candidate -> !candidate.uuid().equals(auction.uuid()))
                .map(AuctionItem::binPrice)
                .sorted()
                .toList();

        if (comparablePrices.size() < 3) {
            return 0L;
        }

        List<Long> nearbyMarket = comparablePrices.stream()
                .filter(price -> price >= Math.round(auction.binPrice() * 1.04D))
                .limit(10)
                .toList();

        if (nearbyMarket.size() >= 3) {
            return median(nearbyMarket);
        }

        int lowClusterEnd = Math.min(comparablePrices.size(), 12);
        return median(comparablePrices.subList(0, lowClusterEnd));
    }

    private long median(List<Long> values) {
        return values.stream()
                .sorted(Comparator.naturalOrder())
                .skip((values.size() - 1L) / 2L)
                .findFirst()
                .orElse(0L);
    }
}
