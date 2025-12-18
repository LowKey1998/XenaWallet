package com.jose.walletapp.helpers.coingecko;

import androidx.annotation.NonNull;

import com.jose.walletapp.constants.Networks;
import com.jose.walletapp.helpers.Token;

import org.json.JSONObject;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CoinGeckoTokenHelper {

    public static String coinIdSolana="solana";
    public static String coinIdBSC="binance-smart-chain";

    public interface TokenCallback {
        void onSuccess(Token token, Set<String> platforms);
        void onError(String error);
    }

    /**
     * Fetch token metadata from CoinGecko using contract/mint address
     *
     * @param coinId           e.g. "solana", "binance-smart-chain", "ethereum"
     * @param contractAddress token contract or mint address
     */
    public static void fetchTokenInfo(
            String coinId,
            String contractAddress,
            String chain,
            TokenCallback callback
    ) {
        // Construct URL for CoinGecko contract API
        String url = "https://api.coingecko.com/api/v3/coins/" + coinId + "/contract/" + contractAddress;

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("CoinGecko request failed");
                    return;
                }

                try {
                    String responseData = response.body().string();
                    JSONObject json = new JSONObject(responseData);

                    // Basic info
                    String id = json.optString("id");
                    String name = json.optString("name");
                    String symbol = json.optString("symbol");

                    // Image
                    String imageUrl = null;
                    if (json.has("image")) {
                        imageUrl = json.getJSONObject("image").optString("large");
                    }

                    Set<String> platformsSet = new HashSet<>();
                    if (json.has("platforms")) {

                        JSONObject platforms = json.getJSONObject("platforms");
                        Iterator<String> keys = platforms.keys();
                        while (keys.hasNext()) {
                            String platform=keys.next();
                            //System.out.println("platform: "+platform);
                            platformsSet.add(platform); // solana, tron, bsc, etc
                        }
                    }

                    // Decimals (Solana uses detail_platforms)
                    int decimals = 0;
                    if (json.has("detail_platforms")) {
                        JSONObject platforms = json.getJSONObject("detail_platforms");

                        // Use first available platform
                        if (platforms.names() != null && platforms.names().length() > 0) {
                            String platformKey = platforms.names().getString(0);
                            JSONObject platform = platforms.getJSONObject(platformKey);
                            decimals = platform.optInt("decimal_place", 0);
                        }
                    }

                    // Construct Token object
                    Token token = new Token(
                            imageUrl,
                            name,
                            symbol.toUpperCase(),
                            chain,
                            contractAddress,
                            id,
                            decimals,
                            false // isStable, adjust if needed
                    );

                    // Return via callback
                    callback.onSuccess(token,platformsSet);

                } catch (Exception e) {
                    callback.onError("Parsing error: " + e.getMessage());
                }
            }
        });
    }

}

