package com.flipradar.market;

import com.flipradar.auction.AuctionItem;
import com.flipradar.auction.AuctionPageFetcher;
import com.flipradar.config.ConfigManager;
import com.flipradar.data.LocalCache;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private volatile List<FlipCandidate> newAlerts = List.of();
    private volatile Instant lastScan = Instant.EPOCH;
    private volatile Instant scanStarted = Instant.EPOCH;
    private volatile String apiStatus = "Idle";
    private volatile int currentPage = 0;
    private volatile int totalPages = 0;
    private volatile int auctionsScanned = 0;
    private volatile int candidatesFound = 0;
    private volatile int candidatesFilteredOut = 0;
    private final Set<String> alertedAuctions = new HashSet<>();
    private final Set<String> alertedSignatures = new HashSet<>();

    public FlipScanner(AuctionPageFetcher pageFetcher, LocalCache cache, ConfigManager configManager) {
        this.pageFetcher = pageFetcher;
        this.configManager = configManager;
    }

    public void refreshAsync() {
        if (!scanning.compareAndSet(false, true)) {
            return;
        }
        scanStarted = Instant.now();
        apiStatus = "Scanning";
        currentPage = 0;
        totalPages = Math.max(1, configManager.get().scanPageLimit);
        auctionsScanned = 0;
        candidatesFound = 0;
        candidatesFilteredOut = 0;

        CompletableFuture.runAsync(() -> {
            List<AuctionItem> auctions = new ArrayList<>();
            try {
                int pageLimit = Math.max(1, configManager.get().scanPageLimit);
                for (int page = 0; page < pageLimit; page++) {
                    int pageIndex = page;
                    pageFetcher.fetchPage(pageIndex).ifPresent(result -> {
                        totalPages = Math.min(pageLimit, result.totalPages());
                        auctions.addAll(result.auctions());
                        auctionsScanned = auctions.size();
                        currentPage = pageIndex + 1;
                    });

                    if (auctions.isEmpty()) {
                        continue;
                    }

                    if (page % 2 == 1 || page == pageLimit - 1 || currentPage >= totalPages) {
                        publishCandidates(auctions);
                    }

                    if (currentPage >= totalPages) {
                        break;
                    }
                }

                publishCandidates(auctions);
                lastScan = Instant.now();
                apiStatus = "OK";
            } catch (Exception exception) {
                apiStatus = "Error";
            } finally {
                scanning.set(false);
            }
        });
    }

    public void refreshIfDue() {
        if (scanning.get()) {
            return;
        }

        int interval = Math.max(60, configManager.get().refreshIntervalSeconds);
        if (lastScan.equals(Instant.EPOCH) || lastScan.plusSeconds(interval).isBefore(Instant.now())) {
            refreshAsync();
        }
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

    public int currentPage() {
        return currentPage;
    }

    public int totalPages() {
        return totalPages;
    }

    public int auctionsScanned() {
        return auctionsScanned;
    }

    public int candidatesFound() {
        return candidatesFound;
    }

    public int candidatesFilteredOut() {
        return candidatesFilteredOut;
    }

    public synchronized List<FlipCandidate> consumeNewAlerts() {
        List<FlipCandidate> alerts = newAlerts;
        newAlerts = List.of();
        return alerts;
    }

    private synchronized List<FlipCandidate> collectNewAlerts(List<FlipCandidate> sorted) {
        List<FlipCandidate> alerts = new ArrayList<>();
        int limit = Math.max(0, configManager.get().maxChatAlertsPerScan);
        for (FlipCandidate candidate : sorted) {
            if (alerts.size() >= limit) {
                break;
            }
            String uuid = candidate.auction().uuid().toString();
            String signature = candidate.auction().signature();
            if (alertedAuctions.add(uuid) && alertedSignatures.add(signature)) {
                alerts.add(candidate);
            }
        }
        return alerts;
    }

    private void publishCandidates(List<AuctionItem> auctions) {
        List<FlipCandidate> candidates = new ArrayList<>();
        int filtered = 0;
        List<AuctionItem> snapshot = List.copyOf(auctions);

        for (AuctionItem auction : snapshot) {
            long value = estimator.estimate(auction, snapshot);
            long tax = profitCalculator.taxFor(value);
            long profit = profitCalculator.profitAfterTax(value, auction.binPrice());
            double profitPercent = profitCalculator.profitPercent(profit, auction.binPrice());
            int confidence = confidenceScorer.score(auction, value, profitPercent, snapshot);
            int ageMinutes = (int) Math.max(0, Duration.between(auction.start(), Instant.now()).toMinutes());
            double volume = Math.min(100.0D, snapshot.stream().filter(a -> a.signature().equals(auction.signature())).count() * 2.0D);

            FlipCandidate candidate = new FlipCandidate(auction, value, tax, profit, profitPercent, confidence, volume, ageMinutes);
            if (filter.include(candidate, configManager.get())) {
                candidates.add(candidate);
            } else {
                filtered++;
            }
        }

        List<FlipCandidate> sorted = candidates.stream()
                .sorted(Comparator.comparingLong(FlipCandidate::profitAfterTax).reversed())
                .limit(configManager.get().maxFlipResults)
                .toList();

        latest = sorted;
        newAlerts = collectNewAlerts(sorted);
        candidatesFound = sorted.size();
        candidatesFilteredOut = filtered;
    }
}
