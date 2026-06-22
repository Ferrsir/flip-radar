package com.flipradar.market;

import com.flipradar.auction.AuctionItem;
import com.flipradar.auction.AuctionPageFetcher;
import com.flipradar.config.ConfigManager;
import com.flipradar.data.LocalCache;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public final class FlipScanner {
    private final AuctionPageFetcher pageFetcher;
    private final ConfigManager configManager;
    private final MarketValueEstimator estimator = new MarketValueEstimator();
    private final ProfitCalculator profitCalculator = new ProfitCalculator();
    private final ConfidenceScorer confidenceScorer = new ConfidenceScorer();
    private final FlipFilter filter = new FlipFilter();
    private final AtomicBoolean scanning = new AtomicBoolean(false);

    private volatile List<FlipCandidate> latest = List.of();
    private volatile Instant lastScan = Instant.EPOCH;
    private volatile String apiStatus = "Idle";

    public FlipScanner(AuctionPageFetcher pageFetcher, LocalCache cache, ConfigManager configManager) {
        this.pageFetcher = pageFetcher;
        this.configManager = configManager;
    }

    public void refreshAsync() {
        if (!scanning.compareAndSet(false, true)) {
            return;
        }
        apiStatus = "Scanning";
        CompletableFuture.runAsync(() -> {
            try {
                List<AuctionItem> auctions = pageFetcher.fetchBinAuctions(8);
                List<FlipCandidate> candidates = new ArrayList<>();
                for (AuctionItem auction : auctions) {
                    long value = estimator.estimate(auction, auctions);
                    long tax = profitCalculator.taxFor(value);
                    long profit = profitCalculator.profitAfterTax(value, auction.binPrice());
                    double profitPercent = profitCalculator.profitPercent(profit, auction.binPrice());
                    int confidence = confidenceScorer.score(auction, value, profitPercent, auctions);
                    int ageMinutes = (int) Math.max(0, Duration.between(auction.start(), Instant.now()).toMinutes());
                    double volume = Math.min(100.0D, auctions.stream().filter(a -> a.signature().equals(auction.signature())).count() * 2.0D);

                    FlipCandidate candidate = new FlipCandidate(auction, value, tax, profit, profitPercent, confidence, volume, ageMinutes);
                    if (filter.include(candidate, configManager.get())) {
                        candidates.add(candidate);
                    }
                }

                latest = candidates.stream()
                        .sorted(Comparator.comparingLong(FlipCandidate::profitAfterTax).reversed())
                        .limit(100)
                        .toList();
                lastScan = Instant.now();
                apiStatus = "OK";
            } catch (Exception exception) {
                apiStatus = "Error";
            } finally {
                scanning.set(false);
            }
        });
    }

    public List<FlipCandidate> latest() {
        return latest;
    }

    public Instant lastScan() {
        return lastScan;
    }

    public String apiStatus() {
        return apiStatus;
    }

    public boolean isScanning() {
        return scanning.get();
    }
}
