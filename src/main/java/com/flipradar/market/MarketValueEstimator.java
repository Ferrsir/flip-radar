package com.flipradar.market;

import com.flipradar.auction.AuctionItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MarketValueEstimator {
    private final BazaarPriceIndex bazaarPriceIndex;

    public MarketValueEstimator(BazaarPriceIndex bazaarPriceIndex) {
        this.bazaarPriceIndex = bazaarPriceIndex;
    }

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
            return componentAdjustedEstimate(auction, activeAuctions);
        }

        int lowClusterSize = Math.min(10, sanePrices.size());
        List<Long> lowestComparableBins = sanePrices.subList(0, lowClusterSize);
        long estimate = conservativePercentile(lowestComparableBins, 40);

        if (isComplexItem(auction) && lowestComparableBins.size() < 8) {
            return componentAdjustedEstimate(auction, activeAuctions);
        }

        long maxSupportedValue = Math.max(auction.binPrice() * 6L, auction.binPrice() + 25_000_000L);
        return Math.min(estimate, maxSupportedValue);
    }

    private long componentAdjustedEstimate(AuctionItem auction, List<AuctionItem> activeAuctions) {
        if (!auction.itemData().hasNbtIdentity()) {
            return 0L;
        }

        List<Long> basePrices = activeAuctions.stream()
                .filter(candidate -> !candidate.uuid().equals(auction.uuid()))
                .filter(candidate -> candidate.itemData().hasNbtIdentity())
                .filter(candidate -> candidate.itemData().skyBlockId().equals(auction.itemData().skyBlockId()))
                .filter(candidate -> candidate.tier().equals(auction.tier()))
                .filter(candidate -> samePetBucket(candidate, auction))
                .map(AuctionItem::binPrice)
                .sorted()
                .toList();

        if (basePrices.size() < 5) {
            return 0L;
        }

        List<Long> saneBasePrices = removeExtremeHighOutliers(basePrices);
        if (saneBasePrices.size() < 5) {
            return 0L;
        }

        long baseValue = conservativePercentile(saneBasePrices.subList(0, Math.min(10, saneBasePrices.size())), 35);
        long componentValue = conservativeComponentValue(auction);
        long estimate = baseValue + componentValue;
        long maxSupportedValue = Math.max(auction.binPrice() * 3L, auction.binPrice() + 15_000_000L);
        return Math.min(estimate, maxSupportedValue);
    }

    private boolean samePetBucket(AuctionItem candidate, AuctionItem auction) {
        if (auction.itemData().petType().isBlank()) {
            return true;
        }
        boolean auctionLevel100 = auction.itemData().petLevel() >= 100;
        boolean candidateLevel100 = candidate.itemData().petLevel() >= 100;
        return candidate.itemData().petType().equals(auction.itemData().petType())
                && candidate.itemData().petTier().equals(auction.itemData().petTier())
                && auctionLevel100 == candidateLevel100;
    }

    private long conservativeComponentValue(AuctionItem auction) {
        long value = 0L;
        if (auction.itemData().recombobulated()) {
            value += Math.round(bazaarPriceIndex.price("RECOMBOBULATOR_3000") * 0.50D);
        }

        int hotPotatoBooks = Math.min(10, auction.itemData().hotPotatoBooks());
        int fumingBooks = Math.max(0, auction.itemData().hotPotatoBooks() - 10);
        value += Math.round(hotPotatoBooks * bazaarPriceIndex.price("HOT_POTATO_BOOK") * 0.25D);
        value += Math.round(fumingBooks * bazaarPriceIndex.price("FUMING_POTATO_BOOK") * 0.25D);
        return value;
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
