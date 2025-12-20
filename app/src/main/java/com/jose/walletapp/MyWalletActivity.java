package com.jose.walletapp;

import static com.jose.walletapp.helpers.ERC20Metadata.callStringFunction;
import static com.jose.walletapp.helpers.ERC20Metadata.callUint8Function;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

//import com.walletconnect.android.internal.common.model.Namespace;
//import com.walletconnect.walletconnectv2.clientsync.session.Session;
//import com.walletconnect.walletconnectv2.clientsync.session.after.params.SessionRequest;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.jose.walletapp.constants.Networks;
import com.jose.walletapp.helpers.ERC20Metadata;
import com.jose.walletapp.helpers.MultiChainWalletManager;
import com.jose.walletapp.helpers.Token;
import com.jose.walletapp.helpers.UnifiedTokenData;
import com.jose.walletapp.helpers.bsc.BscHelper;
import com.jose.walletapp.helpers.coingecko.CoinGeckoTokenHelper;
import com.jose.walletapp.helpers.solana.SolTokenOperations;
import com.jose.walletapp.helpers.user.UserStatus;
import com.melnykov.fab.FloatingActionButton;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import g.p.smartcalculater.R;
import okhttp3.*;

import org.json.JSONObject;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

public class MyWalletActivity extends Activity {
    private static Context context;
    ConcurrentHashMap<String, UnifiedTokenData> tokenMap = new ConcurrentHashMap<>();

    //    private TextView myAddress;
    //private String myAddressStr;
    private String balanceStr;

    private static boolean isNightModeEnabled = false;

    private static final float TRANSLATE_X = 280f;
    private static final float SCALE = 0.88f;

    // 👇 MATCHES IMAGE
    private static final float ROTATE_Y = -8f;   // depth
    private static final float ROTATE_Z = -2f;   // slight tilt
    private TextView totalBalance;
    SwipeRefreshLayout swipeRefreshLayout;
    FloatingActionButton fab;

    private MultiChainWalletManager walletManager;
    private String ethAddress, solAddress, bscAddress;
    private Thread loadTokensThread;
    private LinearLayout tokensListView;
    private CardView contentCard;
    private ImageView profileImage;
    private boolean isSidebarOpen=false;
    //TextView addressTextView = findViewById(R.id.addressTextView);
    //TextView balanceTextView = findViewById(R.id.balanceTextView);

    public interface TokensCallback {
        void onSuccess(List<Token> tokens);
        void onError(String error);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.activity_main);
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        contentCard = findViewById(R.id.contentCard);
        contentCard.animate().setInterpolator(new AccelerateDecelerateInterpolator());

        profileImage = findViewById(R.id.menu);

        profileImage.setOnClickListener(v -> toggleSidebar());

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        tokensListView=findViewById(R.id.token_list);
        totalBalance=findViewById(R.id.balance);


        //set online presense
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference userStatusRef =
                database.getReference("ProductionDB/Users/"+FirebaseAuth.getInstance().getCurrentUser().getUid()+"/Status");

        DatabaseReference connectedRef =
                database.getReference(".info/connected");

        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean connected = snapshot.getValue(Boolean.class);
                if (connected != null && connected) {

                    // Set offline status on disconnect
                    userStatusRef.onDisconnect().setValue(
                            new UserStatus("offline", ServerValue.TIMESTAMP)
                    );

                    // Set online now
                    userStatusRef.setValue(
                            new UserStatus("online", ServerValue.TIMESTAMP)
                    );
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });


        //fetch online status
        /*DatabaseReference statusRef =
                FirebaseDatabase.getInstance().getReference("ProductionDB/Users/"+FirebaseAuth.getInstance().getCurrentUser().getUid()+"Status");

        statusRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String state = snapshot.child("State").getValue(String.class);

                if ("Online".equals(state)) {
                    // show green dot
                } else {
                    // show last seen
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

*/

        fetchUsersDefaultTokensFromFirebase(new TokensCallback(){

            @Override
            public void onSuccess(List<Token> tokens) {
                if(tokens==null) {
                    addDefaultTokensToUserFirebase(FirebaseAuth.getInstance().getCurrentUser().getUid());
                }
                else{
                    LinearLayout defaultTokensListView=findViewById(R.id.default_tokens);
                    for(Token token:tokens){
                        View tokenItemView=MyWalletActivity.this.getLayoutInflater().inflate(R.layout.item_coin_card,null);
                        MyWalletActivity.this.addTokenToDefaultListView(defaultTokensListView,tokenItemView,token.chain);
                    }

                }
            }

            @Override
            public void onError(String error) {

            }
        });


        //shimer
        showLoadingPlaceholders();
        // Fetch tokens immediately from Firebase (offline data will load first)
        //fetchAllTokensFromFirebase();
        // Initial load
        new RefreshWalletTask().execute();

        // Pull to refresh
        swipeRefreshLayout.setOnRefreshListener(() -> {
            new RefreshWalletTask().execute();
        });

        // scrollView=findViewById(R.id.scroll_view);
        /*fab=findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MyWalletActivity.this,AddTokenActivity.class));
            }
        });*/
        //fab.attachToScrollView(scrollView);
        /*findViewById(R.id.depositBtn).setOnClickListener(v -> {
            Intent intent = new Intent(MyWalletActivity.this, DepositActivity.class);
            startActivity(intent);
        });
        findViewById(R.id.withdrawBtn).setOnClickListener(v -> {
            Intent intent = new Intent(MyWalletActivity.this, WithdrawOptionsActivity.class);
            startActivity(intent);
        });*/
        findViewById(R.id.send_button).setOnClickListener(v -> {
            Intent intent = new Intent(MyWalletActivity.this, SendCryptoActivity.class);
            startActivity(intent);
        });
        findViewById(R.id.receive_button).setOnClickListener(v -> {
            Intent intent = new Intent(MyWalletActivity.this, ReceiveActivity.class);
            startActivity(intent);
        });
        /*findViewById(R.id.convertBtn).setOnClickListener(v -> {
            Intent intent = new Intent(MyWalletActivity.this, ConvertActivity.class);
            startActivity(intent);
        });*/


    }

    private void addTokenToDefaultListView(LinearLayout defaultTokensListView, View tokenItemView, String chain) {
        String coinGeckoId = null;
        if(chain.equalsIgnoreCase(Networks.BSC)){
            coinGeckoId="binancecoin";
        } else if (chain.equalsIgnoreCase(Networks.SOLANA)) {
            coinGeckoId = "solana";
        } else if(chain.equalsIgnoreCase(Networks.TRON)){
            coinGeckoId="tron";
        }

        CoinGeckoTokenHelper.fetchTokenInfo(coinGeckoId, null, null, new CoinGeckoTokenHelper.TokenCallback() {
            @Override
            public void onSuccess(Token token, Set<String> platforms) {
/*
                ImageView tokenLogo=tokenItemView.findViewById(R.id.coinIcon);
                Glide.with(MyWalletActivity.this).load(finalUrl)*/
/*.apply(new RequestOptions().circleCrop())*//*
.into(tokenLogo);
*/

                ((TextView)tokenItemView.findViewById(R.id.name)).setText(token.name);
                ((TextView)tokenItemView.findViewById(R.id.symbol)).setText(token.symbol);

                defaultTokensListView.addView(tokenItemView);
            }

            @Override
            public void onError(String error) {

            }
        });
    }

    private void fetchUsersDefaultTokensFromFirebase(TokensCallback callback) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("ProductionDB/Users/" + FirebaseAuth.getInstance().getCurrentUser().getUid() + "/Tokens");

        ref.orderByChild("is_default")
                .equalTo(true)
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        if(snapshot.hasChildren()) {
                            List<Token> tokens = new ArrayList<>();


                            for (DataSnapshot child : snapshot.getChildren()) {
                                Token token = child.getValue(Token.class);
                                if (token != null) {
                                    tokens.add(token);
                                }
                            }

                            callback.onSuccess(tokens);
                        }
                        else{
                            callback.onSuccess(null);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        callback.onError(error.getMessage());
                    }
                });
    }


    private void addDefaultTokensToUserFirebase(String uid) {
        for(String chain:new Networks().getSupportedChains()) {
            DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("ProductionDB/Users/" + FirebaseAuth.getInstance().getCurrentUser().getUid() + "/Tokens");

            // Create a token object
            Map<String, Object> tokenData = new HashMap<>();
            tokenData.put("is_default", true);
            tokenData.put("chain", chain); //can also use "solana", "tron", etc.

            // Push the data to Firebase
            databaseRef.push().setValue(tokenData);
        }
    }

    private void showLoadingPlaceholders() {
        tokensListView.removeAllViews();

        for (int i = 0; i < 8; i++) {
            View shimmerView = getLayoutInflater()
                    .inflate(R.layout.item_token_placeholder, tokensListView, false);
            tokensListView.addView(shimmerView);
        }
    }


    private void toggleSidebar() {
        if (isSidebarOpen) {
            closeSidebar();
        } else {
            openSidebar();
        }
        isSidebarOpen = !isSidebarOpen;
    }

    private void openSidebar() {

        contentCard.setPivotX(0f);
        contentCard.setPivotY(contentCard.getHeight() / 2f);

        contentCard.animate()
                .translationX(320f)
                .scaleX(0.85f)
                .scaleY(0.85f)
                .rotationY(-9f)
                .rotation(-8f)
                .setDuration(320)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        contentCard.setRadius(32f);
    }


    private void closeSidebar() {

        contentCard.setPivotX(contentCard.getWidth() / 2f);
        contentCard.setPivotY(contentCard.getHeight() / 2f);

        contentCard.animate()
                .translationX(0f)
                .scaleX(1f)
                .scaleY(1f)
                .rotationY(0f)
                .rotation(0f)
                .setDuration(280)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        contentCard.setRadius(0f);
    }


    @Override
    public void onBackPressed() {
        if (isSidebarOpen) {
            closeSidebar();
            isSidebarOpen = false;
        } else {
            super.onBackPressed();
        }
    }


    private void fetchAllTokensFromFirebase() {
        Query tokensRef = FirebaseDatabase.getInstance().getReference("ProductionDB/Users/"+ FirebaseAuth.getInstance().getCurrentUser().getUid()+"/Tokens").orderByChild("is_default")
                .equalTo(null);
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
                tokensListView.removeAllViews();
                for (Token token : tokenList) {
                    //Log.d("Token", "Address: " + token.contractAddress + ", Chain: " + token.chain);
                    //LayoutInflater layoutInflater=(LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View tokenItemView=MyWalletActivity.this.getLayoutInflater().inflate(R.layout.item_token,null);
                    if(token.chain.equalsIgnoreCase(Networks.SOLANA)){
                        //unify
                        MyWalletActivity.this.addSolanaTokenToListView(tokensListView,tokenItemView,token.contractAddress);
                    }
                    else if(token.chain.equalsIgnoreCase(Networks.BSC)){
                        MyWalletActivity.this.addBscTokenToListView(tokensListView,tokenItemView,token.contractAddress);
                    }


                }
                //update UI

                // You can now update a RecyclerView, ListView, etc.
            }

            @Override
            public void onCancelled(DatabaseError error) {
                //Toast.makeText(TokenDetailsActivity.this, "Failed to fetch: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addBscTokenToListView(LinearLayout tokensListView, View tokenView, String contractAddress) {
        Web3j web3j = Web3j.build(new HttpService("https://bsc-dataseed.binance.org/"));

        try {
            new Thread(){
                @Override
                public void run() {
                    String name = "";
                    String url = "";
                    String tokenSymbol = "";
                    int decimals = 0;
                    try {
                        name = callStringFunction(web3j, contractAddress, "name");
                        tokenSymbol = callStringFunction(web3j, contractAddress, "symbol");
                        url= ERC20Metadata.fetchLogoFromCoinGecko(Networks.BSC,contractAddress);
                        decimals = callUint8Function(web3j, contractAddress, "decimals");
                    } catch (Exception e) {
                        //throw new RuntimeException(e);
                    }
                    String finalUrl = url;
                    String finalName = name;
                    String finalTokenSymbol = tokenSymbol;
                    int finalDecimals = decimals;
                    runOnUiThread(() ->{
                        ImageView tokenLogo=tokenView.findViewById(R.id.coinIcon);
                        Glide.with(MyWalletActivity.this).load(finalUrl)/*.apply(new RequestOptions().circleCrop())*/.into(tokenLogo);

                        ((TextView)tokenView.findViewById(R.id.coinName)).setText(finalName);
                        ((TextView)tokenView.findViewById(R.id.coinSymbol)).setText(finalTokenSymbol);
                        BscHelper bscHelper=new BscHelper();
                        BigDecimal tokenBalance=bscHelper.getTokenBalance(bscAddress,contractAddress, finalDecimals);

                        ((TextView)tokenView.findViewById(R.id.coinBalanceValue)).setText("$ "+bscHelper.convertToCurrency("tether",tokenBalance,"usd").toString());
                        tokenView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent intent=new Intent(MyWalletActivity.this,TokenDetailsActivity.class);
                                intent.putExtra("contractAddress",contractAddress);
                                intent.putExtra("chain",Networks.BSC);

                                MyWalletActivity.this.startActivity(intent);
                            }
                        });
                        tokensListView.addView(tokenView);

                    });
                }
            }.start();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public void addSolanaTokenToListView(LinearLayout tokens, View tokenView, String contractAddress) {
        String url = "https://api.coingecko.com/api/v3/coins/solana/contract/" + contractAddress;

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    try {
                        JSONObject jsonResponse = new JSONObject(responseData);
                        String logoUrl = jsonResponse.getJSONObject("image").getString("large");
                        String name = jsonResponse.getString("name");
                        String tokenSymbol = jsonResponse.getString("symbol");

                       // addOrUpdateToken(tokenSymbol, Networks.SOLANA, contractAddress, BigDecimal.valueOf(SolTokenOperations.getUserSplTokenBalance(solAddress,contractAddress)) /*BigDecimal.ZERO*/, name, logoUrl);

                        // Handle the token logo URL and name
                        runOnUiThread(() ->{
                            ImageView tokenLogo=tokenView.findViewById(R.id.coinIcon);
                            Glide.with(MyWalletActivity.this).load(logoUrl)/*.apply(new RequestOptions().circleCrop())*/.into(tokenLogo);

                            ((TextView)tokenView.findViewById(R.id.coinName)).setText(name);
                            ((TextView)tokenView.findViewById(R.id.coinSymbol)).setText(tokenSymbol);
                            new Thread(){
                                @Override
                                public void run() {
                                    try {
                                        double balance = SolTokenOperations.getUserSplTokenBalance(/*solAddress*/"4Nd1mZ5n4kL3oHUX77YPDhmPEw24kTx5cHGNy8JD7Q9y",/*contractAddress*/"Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB");
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                ((TextView) tokenView.findViewById(R.id.coinBalanceValue)).setText("" + balance);

                                            }
                                        });
                                    }
                                    catch (Exception e){
                                        e.printStackTrace();

                                    }
                                }
                            }.start();
                            tokenView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Intent intent=new Intent(MyWalletActivity.this,TokenDetailsActivity.class);
                                    intent.putExtra("contractAddress",contractAddress);
                                    intent.putExtra("chain",Networks.SOLANA);

                                    MyWalletActivity.this.startActivity(intent);
                                }
                            });
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
    }//addSolanaTokenToListView


   /* public void addOrUpdateToken(String symbol, String coingecko_coin_id,BigDecimal balance, String name, String logoUrl) {
        String key = coingecko_coin_id;
        tokenMap.compute(key, (k,existing) -> {
            if (existing == null) {
                UnifiedTokenData newToken = new UnifiedTokenData();
                newToken.name = name;
                newToken.symbol = symbol;
                newToken.logoUrl = logoUrl;
                newToken.balancesByChain.put(coingecko_coin_id, balance);
                newToken.totalBalance = balance;
                return newToken;
            } else {
                existing.balancesByChain.put(coingecko_coin_id, balance);
                existing.totalBalance = existing.balancesByChain.values().stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                return existing;
            }
        });
    }*/


    public BigDecimal getGrandTotal() {
        return tokenMap.values().stream()
                .map(token -> token.totalBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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


    private class RefreshWalletTask extends AsyncTask<Void, Void, Double> {

        @Override
        protected void onPreExecute() {
            swipeRefreshLayout.setRefreshing(true);
        }

        @Override
        protected Double doInBackground(Void... voids) {
            try {
                MultiChainWalletManager.getInstance().initialize(MyWalletActivity.this, () -> {
                    ethAddress = MultiChainWalletManager.getInstance().getEthAddress();
                    solAddress = MultiChainWalletManager.getInstance().getSolanaAddress();
                    bscAddress = MultiChainWalletManager.getInstance().getBscAddress();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Toast.makeText(MyWalletActivity.this, myAddressStr, Toast.LENGTH_SHORT).show();
                            Double balance = Double.valueOf(0)/*HdWalletHelper.getSolanaBalance(solAddress)*/;

                            //BigDecimal balanceMatic = HdWalletHelper.getMaticBalance(myAddressStr);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    //swipeRefreshLayout.setRefreshing(false);
                                }
                            });

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
                return 1.0;
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
                return null;
            }
        }

        @Override
        protected void onPostExecute(Double total) {
            if (/*total != null*/false) {
                totalBalance.setText("$" + total);
            } else {
                totalBalance.setText("$ 0");
            }
            swipeRefreshLayout.setRefreshing(false);
        }
    }

}

