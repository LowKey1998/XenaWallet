package com.jose.walletapp.helpers;

public class PriceCalculator {

    public static double toTokenAmount(long rawBalance, int decimals) {
        return rawBalance / Math.pow(10, decimals);
    }

    public static double toUsd(
            double tokenAmount,
            double usdPrice,
            boolean isStable
    ) {
        return isStable ? tokenAmount : tokenAmount * usdPrice;
    }
}

