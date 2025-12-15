package com.jose.walletapp.helpers;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;

public class UnifiedTokenData {
    public final String coingeckoId; // unified ID

    public String name;
    public String symbol;
    public String logoUrl;
    public BigDecimal totalBalance=BigDecimal.ZERO;  // aggregated across chains

    public UnifiedTokenData(String coingeckoId, String symbol, String name, String logoUrl) {
        this.coingeckoId = coingeckoId;
        this.symbol = symbol;
        this.name = name;
        this.logoUrl = logoUrl;
    }

    public ConcurrentHashMap<String, BigDecimal> balancesByChain = new ConcurrentHashMap<>();
}

