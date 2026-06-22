package com.flipradar.config;

import java.util.ArrayList;
import java.util.List;

public final class FlipRadarConfig {
    public long minimumProfitCoins = 1_000_000L;
    public double minimumProfitPercent = 8.0D;
    public int minimumConfidencePercent = 70;
    public double minimumVolumePerDay = 5.0D;
    public int maxAuctionAgeMinutes = 120;
    public long maxPurchasePriceCoins = 50_000_000L;
    public long availableCoins = 0L;
    public boolean usePurseFromAPI = false;
    public boolean useBankFromAPI = false;
    public boolean includeBankInBudget = false;
    public boolean hideItemsAboveBudget = true;
    public List<String> blacklistItems = new ArrayList<>();
    public List<String> whitelistItems = new ArrayList<>();
    public boolean ignoreLowVolumeItems = true;
    public boolean ignoreSkins = true;
    public boolean ignoreExoticsDyedArmor = true;
    public boolean ignoreManipulatedPrices = true;
    public int refreshIntervalSeconds = 180;
    public boolean showDesktopNotifications = false;
    public boolean playSoundOnHighConfidenceDeal = false;
    public boolean confirmBeforeOpeningAuction = true;
    public int scanPageLimit = 60;
    public int maxFlipResults = 500;
    public int maxChatAlertsPerScan = 5;
}
