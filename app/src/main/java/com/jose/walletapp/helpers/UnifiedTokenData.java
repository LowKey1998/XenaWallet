package com.jose.walletapp.helpers;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;

public class UnifiedTokenData {
    public String name;
    public String symbol;
    public String logoUrl;
    public BigDecimal totalBalance;  // aggregated across chains
    public ConcurrentHashMap<String, BigDecimal> balancesByChain = new ConcurrentHashMap<>();
}

