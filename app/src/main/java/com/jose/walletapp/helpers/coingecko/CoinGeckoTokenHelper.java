package com.jose.walletapp.helpers.coingecko;

import androidx.annotation.NonNull;

import com.jose.walletapp.constants.Networks;
import com.jose.walletapp.helpers.Token;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CoinGeckoTokenHelper {

    public interface TokenCallback {
        void onSuccess(Token token);
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
            TokenCallback callback
    ) {

        String url = "https://api.coingecko.com/api/v3/coins/"
                + coinId
                + "/contract/"
                + contractAddress;

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(
                    @NonNull Call call,
                    @NonNull Response response
            ) throws IOException {

                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("CoinGecko request failed");
                    return;
                }

                try {
                    String responseData = response.body().string();
                    JSONObject json = new JSONObject(responseData);

                    // Required fields
                    String id = json.optString("id");
                    String name = json.optString("name");
                    String symbol = json.optString("symbol");

                    // Image
                    String imageUrl = null;
                    if (json.has("image")) {
                        imageUrl = json.getJSONObject("image").optString("large");
                    }

                    // Decimals (Solana uses detail_platforms)
                    int decimals = 0;
                    if (json.has("detail_platforms")) {
                        JSONObject platforms = json.getJSONObject("detail_platforms");

                        // Use first available platform
                        for (int i = 0; i < platforms.names().length(); i++) {
                            String platformKey = platforms.names().getString(i);
                            JSONObject platform = platforms.getJSONObject(platformKey);
                            decimals = platform.optInt("decimal_place", 0);
                            break;
                        }
                    }

                    Token token = new Token(
                            imageUrl,
                            symbol.toUpperCase(),
                            /*chain*/Networks.SOLANA,
                            contractAddress,
                            id,
                            decimals,
                            /*isStable*/false

                    );

                    callback.onSuccess(token);

                } catch (Exception e) {
                    callback.onError("Parsing error: " + e.getMessage());
                }
            }
        });
    }
}

