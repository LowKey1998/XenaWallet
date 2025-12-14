package com.jose.walletapp.helpers;

public class Token {
    public /*final*/ String chain;
    public /*final*/ String symbol;          // SOL, USDT, USDC
    public /*final*/ String logo;
    public /*final*/ String coingeckoId;      // solana, tether, usd-coin
    public /*final*/ int decimals;           // 9 for SOL, 6 for USDC
    public /*final*/ boolean isStable;  // true for USDT/USDC

    public /*final*/ String contractAddress;

    public Token() {}

    public Token(String logo, String symbol, String chain, String contractAddress, String coingeckoId, int decimals, boolean isStable) {
        this.logo = logo;
        this.symbol = symbol;
        this.contractAddress = contractAddress;
        this.chain = chain;
        this.coingeckoId = coingeckoId;
        this.decimals = decimals;
        this.isStable = isStable;
    }

}
