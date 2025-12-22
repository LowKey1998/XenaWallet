package com.jose.walletapp.helpers;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PriceCalculator {

    public static double toTokenAmount(long rawBalance, int decimals) {
        return rawBalance / Math.pow(10, decimals);
    }

    public static double toUsd(
            double tokenAmount,
            double usdPrice,
            boolean isStable
    ) {
        return isStable ? tokenAmount : BigDecimal.valueOf(tokenAmount)
                .multiply(BigDecimal.valueOf(usdPrice))
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}

