package com.jose.walletapp;

import static com.jose.walletapp.helpers.ERC20Metadata.callStringFunction;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.github.mikephil.charting.charts.LineChart;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.jose.walletapp.constants.Networks;
import com.jose.walletapp.helpers.ERC20Metadata;
import com.jose.walletapp.helpers.MultiChainWalletManager;
import com.jose.walletapp.helpers.Token;
import com.jose.walletapp.helpers.bsc.BscHelper;
import com.jose.walletapp.helpers.coingecko.CoinGeckoTokenHelper;

import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import g.p.smartcalculater.R;

public class TokenDetailsActivity extends Activity {

    private TextView title, symbol,balanceZMW;
    private ImageView logo;
    private LineChart priceChart;
    private ListView transactionList;

    private String chainId;
    private String contractAddress;
    private String userWalletAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_wallet);

        // UI elements
        title = findViewById(R.id.title);
        symbol = findViewById(R.id.symbol);
        logo = findViewById(R.id.token_logo);
        balanceZMW = findViewById(R.id.token_balance);
        //transactionList = findViewById(R.id.transaction_list);


        // Suppose your XML has ImageView with id token_logo and qr_code
       // tokenLogo = findViewById(R.id.token_logo);
       // qrCodeView = findViewById(R.id.qr_code);
        //priceChart = findViewById(R.id.price_chart);


        // Get intent extras
        chainId = getIntent().getStringExtra("chain");
        contractAddress = getIntent().getStringExtra("contractAddress");


        findViewById(R.id.send_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String coinGeckoFetchChainId = "";
                if(chainId.equalsIgnoreCase(Networks.SOLANA)){
                    coinGeckoFetchChainId=CoinGeckoTokenHelper.coinIdSolana;
                }
                else if (chainId.equalsIgnoreCase(Networks.BSC)) {
                    coinGeckoFetchChainId=CoinGeckoTokenHelper.coinIdBSC;
                }
                CoinGeckoTokenHelper.fetchTokenInfo(coinGeckoFetchChainId,
                        contractAddress, chainId, new CoinGeckoTokenHelper.TokenCallback() {
                            @Override
                            public void onSuccess(Token token, Set<String> platforms) {
                                String coinGeckoId = token.coingeckoId; // nullable
                                String baseChain = token.chain; // e.g., "Solana", "BSC", "TRC20"

                                Networks networks = new Networks();
                                List<String> supportedChains = networks.getSupportedChains();

                                if (coinGeckoId == null || coinGeckoId.isEmpty()) {
                                    // Fallback: only base chain
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            showSendBottomSheet(
                                                    TokenDetailsActivity.this,
                                                    Collections.singletonList(baseChain)
                                            );
                                        }
                                    });

                                    return;
                                }

                                List<String> available = new ArrayList<>();

                                for (String chain : supportedChains) {
                                    //System.out.println("Chain: "+chain);
                                    // Compare CoinGecko keys to app chains
                                    // Example mapping: Solana -> solana, BSC -> binance-smart-chain
                                    String cgKey = chain.equalsIgnoreCase(Networks.SOLANA) ? "solana"
                                            : chain.equalsIgnoreCase(Networks.BSC) ? "binance-smart-chain"
                                            : chain.equalsIgnoreCase(Networks.TRON) ? "tron"
                                            : "";

                                    //System.out.println("CgKey: "+cgKey);

                                    /*for(String platform:platforms){
                                        System.out.println("Platform:"+platform);
                                    }*/
                                    if (platforms.contains(cgKey)) {
                                       //System.out.println("chain: "+chain);
                                        available.add(chain);
                                    }
                                }

                                // Fallback to base chain if none matched
                                if (available.isEmpty()) {
                                    available.add(baseChain);
                                }

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        showSendBottomSheet(TokenDetailsActivity.this, available);

                                    }
                                });


                            }

                            @Override
                            public void onError(String error) {

                            }
                });



            }
        });
        findViewById(R.id.receive_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String coinGeckoFetchChainId = "";
                if(chainId.equalsIgnoreCase(Networks.SOLANA)){
                    coinGeckoFetchChainId=CoinGeckoTokenHelper.coinIdSolana;
                }
                else if (chainId.equalsIgnoreCase(Networks.BSC)) {
                    coinGeckoFetchChainId=CoinGeckoTokenHelper.coinIdBSC;
                }
                CoinGeckoTokenHelper.fetchTokenInfo(coinGeckoFetchChainId,
                        contractAddress, chainId, new CoinGeckoTokenHelper.TokenCallback() {
                            @Override
                            public void onSuccess(Token token, Set<String> platforms) {
                                String coinGeckoId = token.coingeckoId; // nullable
                                String baseChain = token.chain; // e.g., "Solana", "BSC", "TRC20"

                                Networks networks = new Networks();
                                List<String> supportedChains = networks.getSupportedChains();

                                if (coinGeckoId == null || coinGeckoId.isEmpty()) {
                                    // Fallback: only base chain
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            showReceiveBottomSheet(
                                                    TokenDetailsActivity.this,
                                                    Collections.singletonList(baseChain)
                                            );
                                        }
                                    });

                                    return;
                                }

                                List<String> available = new ArrayList<>();

                                for (String chain : supportedChains) {
                                    //System.out.println("Chain: "+chain);
                                    // Compare CoinGecko keys to app chains
                                    // Example mapping: Solana -> solana, BSC -> binance-smart-chain
                                    String cgKey = chain.equalsIgnoreCase(Networks.SOLANA) ? "solana"
                                            : chain.equalsIgnoreCase(Networks.BSC) ? "binance-smart-chain"
                                            : chain.equalsIgnoreCase(Networks.TRON) ? "tron"
                                            : "";

                                    //System.out.println("CgKey: "+cgKey);

                                    /*for(String platform:platforms){
                                        System.out.println("Platform:"+platform);
                                    }*/
                                    if (platforms.contains(cgKey)) {
                                       //System.out.println("chain: "+chain);
                                        available.add(chain);
                                    }
                                }

                                // Fallback to base chain if none matched
                                if (available.isEmpty()) {
                                    available.add(baseChain);
                                }

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        showReceiveBottomSheet(TokenDetailsActivity.this, available);

                                    }
                                });


                            }

                            @Override
                            public void onError(String error) {

                            }
                });



            }
        });

        if(chainId.equalsIgnoreCase(Networks.SOLANA)) {
            userWalletAddress = MultiChainWalletManager.getInstance().getSolanaAddress();
        }
        else if (chainId.equalsIgnoreCase(Networks.BSC)) {
            userWalletAddress = MultiChainWalletManager.getInstance().getBscAddress();

        }
        else if (chainId.equalsIgnoreCase(Networks.TRON)) {
            //userWalletAddress = MultiChainWalletManager.getInstance().getEthAddress();

        }

        // Fetch token details
        fetchTokenDetails();
    }

    private void fetchTokenDetails() {
        if(chainId.equalsIgnoreCase(Networks.BSC)) {
            try {
                new Thread(){
                    @Override
                    public void run() {
                        JSONObject token=new BscHelper().getTokenInfo(contractAddress);
                        runOnUiThread(() ->{
                            try {
                                title.setText(token.getString("name"));
                                symbol.setText(token.getString("symbol"));
                                try {
                                    /*Glide.with(TokenDetailsActivity.this).load(token.getString("logo"))
                                            .apply(new RequestOptions().circleCrop()).into(logo);*/
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            String url = null;
                                            try {
                                                url = ERC20Metadata.fetchLogoFromCoinGecko(Networks.BSC, contractAddress);
                                                String finalUrl = url;
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Glide.with(TokenDetailsActivity.this).load(finalUrl)
                                                                .apply(new RequestOptions().circleCrop()).into(logo);
                                                    }
                                                });
                                            } catch (Exception ex) {
                                                //throw new RuntimeException(ex);
                                            }


                                        }
                                    }).start();
                                } catch (Exception e) {
                                    /*try {
                                        new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                String url = null;
                                                try {
                                                    url = ERC20Metadata.fetchLogoFromCoinGecko(Networks.BSC, contractAddress);
                                                    String finalUrl = url;
                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            Glide.with(TokenDetailsActivity.this).load(finalUrl)
                                                                    .apply(new RequestOptions().circleCrop()).into(logo);
                                                        }
                                                    });
                                                } catch (Exception ex) {
                                                    //throw new RuntimeException(ex);
                                                }


                                            }
                                        }).start();
                                    }
                                    catch (Exception se){

                                    }*/

                                }

                            } catch (JSONException e) {
                                //Log.e("Error",e.toString());
                                //throw new RuntimeException(e);
                            }
                            //symbolEditText.setText(tokenSymbol);
                        });

                    }
                }.start();

            } catch (Exception e) {
               // throw new RuntimeException(e);
            }
        }
        else if(chainId.equalsIgnoreCase(Networks.SOLANA)){
            //fetchSolanaTokenMetadata(contractAddress);
            String coinGeckoFetchChainId = "";
            if(chainId.equalsIgnoreCase(Networks.SOLANA)){
                coinGeckoFetchChainId=CoinGeckoTokenHelper.coinIdSolana;
            }
            else if (chainId.equalsIgnoreCase(Networks.BSC)) {
                coinGeckoFetchChainId=CoinGeckoTokenHelper.coinIdBSC;
            }
            CoinGeckoTokenHelper.fetchTokenInfo(coinGeckoFetchChainId, contractAddress, chainId, new CoinGeckoTokenHelper.TokenCallback() {
                @Override
                public void onSuccess(Token token, Set<String> platforms) {
                    runOnUiThread(() ->{
                        title.setText(token.name);
                        symbol.setText(token.symbol);
                        Glide.with(TokenDetailsActivity.this).load(token.logo)
                                .apply(new RequestOptions().circleCrop()).into(logo);

                    });

                }

                @Override
                public void onError(String error) {

                }
            });
        }
    }

    private void displayToken(TokenData tokenData) {
        title.setText(tokenData.name);
        balanceZMW.setText("ZMW " + tokenData.balance.toPlainString());

        // Load logo (use Glide/Picasso)
        //Glide.with(this).load(tokenData.logoUrl).into(tokenLogo);

        // Draw price chart (dummy example)
        drawChart(tokenData);

        // Populate transaction list (if you have transaction fetching)
        // transactionList.setAdapter(new TransactionAdapter(...));
    }


    private void showReceiveBottomSheet(Context context, List<String> chains) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context)
                .inflate(R.layout.activity_network_selection, null);

        LinearLayout list = view.findViewById(R.id.network_list);
        for (int i = 0; i < chains.size(); i++) {
            String chain = chains.get(i);
            View networkItem = getLayoutInflater().inflate(R.layout.item_network, null);
            ((TextView) networkItem.findViewById(R.id.name)).setText(chain);
            //((TextView)networkItem.findViewById(R.id.name_symbol)).setText();
            networkItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                    // Navigate to your QR / receive screen
                    Intent intent = new Intent(TokenDetailsActivity.this, ReceiveActivity.class);
                    intent.putExtra("contractAddress", contractAddress);
                    intent.putExtra("chain", chain);
                    startActivity(intent);
                }
            });
            list.addView(networkItem);
        }

        dialog.setContentView(view);
        dialog.show();
    }


    private void showSendBottomSheet(Context context, List<String> chains) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context)
                .inflate(R.layout.activity_network_selection, null);

        LinearLayout list = view.findViewById(R.id.network_list);
        for (int i = 0; i < chains.size(); i++) {
            String chain = chains.get(i);
            View networkItem = getLayoutInflater().inflate(R.layout.item_network, null);
            ((TextView) networkItem.findViewById(R.id.name)).setText(chain);
            //((TextView)networkItem.findViewById(R.id.name_symbol)).setText();
            networkItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                    // Navigate to your QR / receive screen
                    Intent intent = new Intent(TokenDetailsActivity.this, SendCryptoActivity.class);
                    intent.putExtra("contractAddress", contractAddress);
                    intent.putExtra("chain", chain);
                    startActivity(intent);
                }
            });
            list.addView(networkItem);
        }

        dialog.setContentView(view);
        dialog.show();
    }


    private void drawChart(TokenData tokenData) {
        /*List<Entry> entries = new ArrayList<>();
        // Dummy data for illustration
        entries.add(new Entry(0, tokenData.balance.floatValue()));
        entries.add(new Entry(1, tokenData.balance.floatValue().multiply(new BigDecimal("1.05")).floatValue()));
        entries.add(new Entry(2, tokenData.balance.floatValue().multiply(new BigDecimal("1.1")).floatValue()));

        LineDataSet dataSet = new LineDataSet(entries, tokenData.symbol + " Price");
        LineData lineData = new LineData(dataSet);
        priceChart.setData(lineData);
        priceChart.invalidate();*/ // refresh
    }

    private String getCoinGeckoTokenApi(String chainId, String contractAddress) {
        // Map chainId to CoinGecko network slug
        String network = "ethereum";
        if (chainId.equalsIgnoreCase("bsc")) network = "binance-smart-chain";
        else if (chainId.equalsIgnoreCase("sol")) network = "solana";

        return "https://api.coingecko.com/api/v3/onchain/networks/" + network + "/tokens/" + contractAddress;
    }

    // Simple token data holder
    private static class TokenData {
        String symbol;
        String name;
        String logoUrl;
        BigDecimal balance;
        String coingeckoId;
        int decimals;

        TokenData(String symbol, String name, String logoUrl, BigDecimal balance, String coingeckoId, int decimals) {
            this.symbol = symbol;
            this.name = name;
            this.logoUrl = logoUrl;
            this.balance = balance;
            this.coingeckoId = coingeckoId;
            this.decimals = decimals;
        }
    }
}
