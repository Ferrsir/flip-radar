package com.flipradar.market;

public final class ProfitCalculator {
    public long taxFor(long sellPrice) {
        if (sellPrice <= 0L) {
            return 0L;
        }
        double taxRate = sellPrice >= 100_000_000L ? 0.025D : 0.02D;
        return Math.round(sellPrice * taxRate);
    }

    public long profitAfterTax(long estimatedMarketValue, long binPrice) {
        return estimatedMarketValue - binPrice - taxFor(estimatedMarketValue);
    }

    public double profitPercent(long profitAfterTax, long binPrice) {
        if (binPrice <= 0L) {
            return 0.0D;
        }
        return (profitAfterTax * 100.0D) / binPrice;
    }
}
