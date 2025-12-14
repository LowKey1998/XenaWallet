package com.jose.walletapp;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

//import com.walletconnect.android.internal.common.model.Namespace;
//import com.walletconnect.walletconnectv2.clientsync.session.Session;
//import com.walletconnect.walletconnectv2.clientsync.session.after.params.SessionRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jose.walletapp.constants.Networks;
import com.jose.walletapp.helpers.HdWalletHelper;
import com.jose.walletapp.helpers.MultiChainWalletManager;
import com.jose.walletapp.helpers.Token;
import com.melnykov.fab.FloatingActionButton;
import com.walletconnect.web3.wallet.client.Wallet;
import com.walletconnect.web3.wallet.client.Web3Wallet;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import g.p.smartcalculater.R;
import kotlin.Unit;
import kotlin.jvm.functions.Function2;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Text;

public class MyWalletActivity extends Activity {
    private static Context context;
//    private TextView myAddress;
    //private String myAddressStr;
    private String balanceStr;

    private static boolean isNightModeEnabled = false;
    private TextView totalBalance;
    private TextView totalMaticBalance;
    SwipeRefreshLayout swipeRefreshLayout;
    FloatingActionButton fab;
    private Thread checkBalanceThread;
    private LinearLayout usdtAsset;
    private MultiChainWalletManager walletManager;
    private String ethAddress, solAddress, bscAddress;
    private Thread loadTokensThread;
    private LinearLayout tokensListView;
    //TextView addressTextView = findViewById(R.id.addressTextView);
    //TextView balanceTextView = findViewById(R.id.balanceTextView);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.activity_main);
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        tokensListView=findViewById(R.id.tokens);
        fab=findViewById(R.id.fab);
       // scrollView=findViewById(R.id.scroll_view);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MyWalletActivity.this,AddTokenActivity.class));
            }
        });
        //fab.attachToScrollView(scrollView);
        findViewById(R.id.depositBtn).setOnClickListener(v -> {
            Intent intent = new Intent(MyWalletActivity.this, DepositActivity.class);
            startActivity(intent);
        });
        findViewById(R.id.withdrawBtn).setOnClickListener(v -> {
            Intent intent = new Intent(MyWalletActivity.this, WithdrawOptionsActivity.class);
            startActivity(intent);
        });
        findViewById(R.id.sendBtn).setOnClickListener(v -> {
            Intent intent = new Intent(MyWalletActivity.this, SendActivity.class);
            startActivity(intent);
        });
        findViewById(R.id.receiveBtn).setOnClickListener(v -> {
            Intent intent = new Intent(MyWalletActivity.this, ReceiveActivity.class);
            startActivity(intent);
        });
        findViewById(R.id.convertBtn).setOnClickListener(v -> {
            Intent intent = new Intent(MyWalletActivity.this, ConvertActivity.class);
            startActivity(intent);
        });


        try {
            MultiChainWalletManager.getInstance().initialize(this, () -> {
                ethAddress = MultiChainWalletManager.getInstance().getEthAddress();
                solAddress = MultiChainWalletManager.getInstance().getSolanaAddress();
                bscAddress = MultiChainWalletManager.getInstance().getBscAddress();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                       // myAddress=findViewById(R.id.myAddress);

                       // Toast.makeText(MyWalletActivity.this, solAddress, Toast.LENGTH_SHORT).show();

                        //ToDo:add statement to check if null
                       // myAddressStr= /*HdWalletHelper.getMyAddress(context);*/solAddress;
                        Toast.makeText(MyWalletActivity.this, solAddress, Toast.LENGTH_SHORT).show();
                       // myAddress.setText(myAddressStr);
                        /*myAddress.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                ClipData clip = ClipData.newPlainText("Wallet Address", myAddressStr);
                                clipboard.setPrimaryClip(clip);

                                Toast.makeText(MyWalletActivity.this, "Address copied to clipboard", Toast.LENGTH_SHORT).show();
                            }
                        });*/
                        totalBalance=findViewById(R.id.combinedTotalAmount);


                        checkBalanceThread=new Thread(){
                            @Override
                            public void run() {
                                try {

                                    //Toast.makeText(MyWalletActivity.this, myAddressStr, Toast.LENGTH_SHORT).show();
                                    Double balance = HdWalletHelper.getSolanaBalance(solAddress);

                                    //BigDecimal balanceMatic = HdWalletHelper.getMaticBalance(myAddressStr);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {

                                            totalBalance.setText(balance != null ? ("$" + balance) : "Error ");
                                            //totalMaticBalance.setText(balanceMatic != null ? ("$" + balanceMatic) : "Error fetching Balance");
                                            swipeRefreshLayout.setRefreshing(false);
                                        }
                                    });
                                }
                                catch (Exception e){
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            swipeRefreshLayout.setRefreshing(false);
                                        }
                                    });
                                }
                            }
                        };

                        fetchAllTokensFromFirebase();
                        loadTokensThread=new Thread(){
                            @Override
                            public void run() {
                                try {


                                    //BigDecimal balanceMatic = HdWalletHelper.getMaticBalance(myAddressStr);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {

                                            swipeRefreshLayout.setRefreshing(false);
                                        }
                                    });
                                }
                                catch (Exception e){
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            swipeRefreshLayout.setRefreshing(false);
                                        }
                                    });
                                }
                            }
                        };

                        // Initial load
                        swipeRefreshLayout.setRefreshing(true);
                        checkBalanceThread.start();

                        // Pull-to-refresh handler
                        swipeRefreshLayout.setOnRefreshListener(() -> checkBalanceThread.start());

                        // totalBalance.setText(balanceStr!=null?("$"+balanceStr):"Problem fetching balance");


                    }
                });
               // Log.d("WALLET", "ETH: " + eth);
              //  Log.d("WALLET", "SOL: " + sol);
                //Log.d("WALLET", "BSC: " + bsc);
            }, () -> {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Wallet initialization failed", Toast.LENGTH_SHORT).show();

                    }
                });
                Log.e("WALLET", "Wallet initialization failed");
            });
        }
        catch (Exception e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Wallet creation failed", Toast.LENGTH_SHORT).show();
                }
            });
            e.printStackTrace();
            Log.e("Wallet", "Wallet creation failed: " + e.getMessage());
        }

        /*Web3Wallet.INSTANCE.setWalletDelegate(new Web3Wallet.WalletDelegate() {
            @Override
            public void onSessionRequest(@NonNull Wallet.Model.SessionRequest sessionRequest) {

            }

            @Override
            public void onSessionProposal(@NonNull Wallet.Model.SessionProposal sessionProposal) {
                // Show UI to user to approve or reject
                *//*Namespace.Session namespace = new Namespace.Session(
                        Collections.singletonList("eip155:56:<YOUR_WALLET_ADDRESS>"),
                        sessionProposal.getRequiredNamespaces().get("eip155").getMethods(),
                        sessionProposal.getRequiredNamespaces().get("eip155").getEvents()
                );

                Map<String, Namespace.Session> namespaces = new HashMap<>();
                namespaces.put("eip155", namespace);

                Web3Wallet.INSTANCE.approveSession(sessionProposal, namespaces);*//*

            }

            @Override
            public void onAuthRequest(@NonNull Wallet.Model.AuthRequest authRequest) {

            }

            @Override
            public void onError(@NonNull Wallet.Model.Error error) {

            }

            @Override
            public void onConnectionStateChange(@NonNull Wallet.Model.ConnectionState connectionState) {

            }



            @Override
            public void onSessionUpdateResponse(@NonNull Wallet.Model.SessionUpdateResponse sessionUpdateResponse) {

            }

            @Override
            public void onSessionSettleResponse(@NonNull Wallet.Model.SettledSessionResponse settledSessionResponse) {

            }


            @Override
            public void onSessionDelete(@NonNull Wallet.Model.SessionDelete sessionDelete) {

            }


        });

*/

    }


    private void fetchAllTokensFromFirebase() {
        DatabaseReference tokensRef = FirebaseDatabase.getInstance().getReference("ProductionDB/Users/"+ FirebaseAuth.getInstance().getCurrentUser().getUid()+"/Tokens");

        tokensRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<Token> tokenList = new ArrayList<>();

                for (DataSnapshot tokenSnapshot : snapshot.getChildren()) {
                    Token token = tokenSnapshot.getValue(Token.class);
                    if (token != null) {
                        tokenList.add(token);
                    }
                }

                // ✅ Now you have all tokens in tokenList
                for (Token token : tokenList) {
                    //Log.d("Token", "Address: " + token.contractAddress + ", Chain: " + token.chain);
                    //LayoutInflater layoutInflater=(LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View tokenItemView=MyWalletActivity.this.getLayoutInflater().inflate(R.layout.item_token,null);
                    if(token.chain.equals(Networks.SOLANA)){

                        MyWalletActivity.this.addSolanaTokenToListView(tokensListView,tokenItemView,token.contractAddress);
                    }
                    else if(token.chain.equals(Networks.BSC)){

                    }


                }

                // You can now update a RecyclerView, ListView, etc.
            }

            @Override
            public void onCancelled(DatabaseError error) {
                //Toast.makeText(TokenDetailsActivity.this, "Failed to fetch: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    public void addSolanaTokenToListView(LinearLayout tokens, View tokenView, String contractAddress) {
        String url = "https://api.coingecko.com/api/v3/coins/solana/contract/" + contractAddress;

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    try {
                        JSONObject jsonResponse = new JSONObject(responseData);
                        String logoUrl = jsonResponse.getJSONObject("image").getString("small");
                        String name = jsonResponse.getString("name");
                        String tokenSymbol = jsonResponse.getString("symbol");

                        // Handle the token logo URL and name
                        runOnUiThread(() ->{
                            ImageView tokenLogo=tokenView.findViewById(R.id.coinIcon);
                            Glide.with(MyWalletActivity.this).load(logoUrl)/*.apply(new RequestOptions().circleCrop())*/.into(tokenLogo);

                            ((TextView)tokenView.findViewById(R.id.coinName)).setText(name);
                            ((TextView)tokenView.findViewById(R.id.coinSymbol)).setText(tokenSymbol);
                            tokens.addView(tokenView);

                        });
                    } catch (Exception e) {
                        e.printStackTrace();/*
                        runOnUiThread(() -> Toast.makeText(AddTokenActivity.this,
                                "Error parsing token metadata", Toast.LENGTH_SHORT).show());*/
                    }
                } else {/*
                    runOnUiThread(() -> Toast.makeText(AddTokenActivity.this,
                            "Error fetching token metadata", Toast.LENGTH_SHORT).show());*/
                }
            }


            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {/*
                runOnUiThread(() -> Toast.makeText(AddTokenActivity.this,
                        "Failed to fetch token metadata", Toast.LENGTH_SHORT).show());*/
            }

        });
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleWalletConnectUri(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handleWalletConnectUri(getIntent());
    }

    private void handleWalletConnectUri(Intent intent) {
        Uri data = intent.getData();
        if (data != null && "wc".equals(data.getHost())) {
            String wcUri = data.getQueryParameter("uri");
            if (wcUri != null) {
               /* Web3Wallet.INSTANCE.pair(wcUri, result -> {
                    Log.d("WalletConnect", "Paired with dApp");
                    return null;
                }, error -> {
                    Log.e("WalletConnect", "Pairing failed: " *//*+ error.getMessage()*//*);
                    return null;
                });*/
            }
        }
    }


    public void fetchUsdtTransactions(String walletAddress) {
        LinearLayout transactionsLinearLayout=findViewById(R.id.transactions);

        String apiKey = "YOUR_API_KEY";//todo:get api key from snowtrace
        String url = "https://api.snowtrace.io/api"
                + "?module=account"
                + "&action=tokentx"
                + "&contractaddress=0xc7198437980c041c805A1EDcbA50c1Ce5db95118"
                + "&address=" + walletAddress
                + "&page=1"
                + "&offset=20"
                + "&sort=desc"
                + "&apikey=" + apiKey;

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String json = response.body().string();
                        JSONObject result = new JSONObject(json);
                        JSONArray transactions = result.getJSONArray("result");

                        for (int i = 0; i < transactions.length(); i++) {
                            JSONObject tx = transactions.getJSONObject(i);
                            String hash = tx.getString("hash");
                            String from = tx.getString("from");
                            String to = tx.getString("to");
                            String value = tx.getString("value");
                            String status = tx.getString("txreceipt_status"); // 1 = success, 0 = fail

                            Log.d("TX", "Hash: " + hash + ", Status: " + status);
                            //add transtaction to UI

                        }//for
                    }
                    catch (Exception e){}
                }
                else{

                }
            }//onResponse
        });
    }


}

