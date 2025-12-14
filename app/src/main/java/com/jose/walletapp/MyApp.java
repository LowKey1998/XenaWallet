package com.jose.walletapp;

import static com.walletconnect.web3.wallet.client.Web3Wallet.coreClient;

import android.app.Application;

import com.walletconnect.android.Core;
//import com.walletconnect.android.internal.common.model.AppMetaData;
import com.walletconnect.android.relay.ConnectionType;
//import com.walletconnect.walletconnectv2.common.AppMetaData;
import com.walletconnect.web3.wallet.client.Wallet;
import com.walletconnect.web3.wallet.client.Web3Wallet;
//import com.walletconnect.web3.wallet.client.Web3Wallet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        List<String> iconList = Collections.singletonList("https://mywallet.com/icon.png");

       /* AppMetaData metaData = new AppMetaData(
                "MyWalletApp",
                "Secure crypto wallet",
                "https://mywallet.com",
                iconList*//*,
                "mywallet://"*//*
        );*/



        Wallet.Params.Init initParams = new Wallet.Params.Init(coreClient);
        Web3Wallet.INSTANCE.initialize(
                initParams,error -> {
                    return null;
                }
                //"wss://relay.walletconnect.com",
                //ConnectionType.AUTOMATIC
        );
    }
}

