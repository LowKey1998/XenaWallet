package com.jose.walletapp.constants;

import java.util.Collection;
import java.util.List;

public class Networks {
    //public static final String ETHEREUM = "Ethereum";
    public static final String SOLANA = "Solana";
    public static final String BSC = "BSC";
    public static final String TRON = "TRC20";


    public List<String> getSupportedChains() {
        return List.of(SOLANA, BSC, TRON);
    }

}
