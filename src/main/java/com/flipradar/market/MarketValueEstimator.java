package com.flipradar.market;

import com.flipradar.auction.AuctionItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MarketValueEstimator {
    public long estimate(AuctionItem auction, List<AuctionItem> activeAuctions) {
        List<Long> comparablePrices = activeAuctions.stream()
                .filter(candidate -> candidate.signature().equals(auction.signature()))
                .filter(candidate -> !candidate.uuid().equals(auction.uuid()))
                .map(AuctionItem::binPrice)
                .sorted()
                .toList();

        if (comparablePrices.size() < 5) {
            return 0L;
        }

        List<Long> sanePrices = removeExtremeHighOutliers(comparablePrices);
        if (sanePrices.size() < 5) {
            return 0L;
        }

        int lowClusterSize = Math.min(8, sanePrices.size());
        List<Long> lowestComparableBins = sanePrices.subList(0, lowClusterSize);
        long estimate = conservativePercentile(lowestComparableBins, 60);

        if (isComplexItem(auction) && lowestComparableBins.size() < 8) {
            return 0L;
        }

        long maxSupportedValue = Math.max(auction.binPrice() * 6L, auction.binPrice() + 25_000_000L);
        return Math.min(estimate, maxSupportedValue);
    }

    private List<Long> removeExtremeHighOutliers(List<Long> sortedPrices) {
        if (sortedPrices.size() < 5) {
            return sortedPrices;
        }

        long anchor = sortedPrices.get(Math.min(2, sortedPrices.size() - 1));
        long hardCap = Math.max(anchor * 4L, anchor + 50_000_000L);
        List<Long> filtered = new ArrayList<>();
        for (long price : sortedPrices) {
            if (price <= hardCap) {
                filtered.add(price);
            }
        }
        return filtered;
    }

    private long conservativePercentile(List<Long> sortedValues, int percentile) {
        int index = Math.min(sortedValues.size() - 1, Math.max(0, (int) Math.ceil(sortedValues.size() * (percentile / 100.0D)) - 1));
        return sortedValues.get(index);
    }

    private boolean isComplexItem(AuctionItem auction) {
        String signature = auction.signature().toLowerCase(Locale.ROOT);
        return signature.contains("recombobulated")
                || signature.contains("gemstones")
                || signature.contains("attributes")
                || !signature.contains("enchants:0");
    }
}
