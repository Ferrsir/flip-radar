package com.flipradar.market;

import com.flipradar.config.FlipRadarConfig;

import java.util.Locale;

public final class FlipFilter {
    public boolean include(FlipCandidate candidate, FlipRadarConfig config) {
        if (candidate.estimatedMarketValue() <= 0L) {
            return false;
        }
        if (config.hideItemsAboveBudget && config.availableCoins > 0L && candidate.auction().binPrice() > config.availableCoins) {
            return false;
        }
        if (candidate.auction().binPrice() > config.maxPurchasePriceCoins) {
            return false;
        }
        if (candidate.profitAfterTax() < config.minimumProfitCoins) {
            return false;
        }
        if (candidate.profitPercent() < config.minimumProfitPercent) {
            return false;
        }
        if (candidate.confidencePercent() < config.minimumConfidencePercent) {
            return false;
        }
        if (config.ignoreLowVolumeItems && candidate.volumePerDay() < config.minimumVolumePerDay) {
            return false;
        }
        String itemName = candidate.auction().itemName().toLowerCase(Locale.ROOT);
        if (config.blacklistItems.stream().anyMatch(item -> itemName.contains(item.toLowerCase(Locale.ROOT)))) {
            return false;
        }
        if (!config.whitelistItems.isEmpty()
                && config.whitelistItems.stream().noneMatch(item -> itemName.contains(item.toLowerCase(Locale.ROOT)))) {
            return false;
        }
        return true;
    }
}
