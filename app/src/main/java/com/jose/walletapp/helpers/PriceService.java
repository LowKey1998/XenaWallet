package com.jose.walletapp.helpers;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PriceService {

    private static final String BASE_URL =
            "https://api.coingecko.com/api/v3/simple/price";

    public static void getUsdPrice(
            String coingeckoId,
            PriceCallback callback
    ) {
        OkHttpClient client = new OkHttpClient();

        String url = BASE_URL +
                "?ids=" + coingeckoId +
                "&vs_currencies=usd";

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                JSONObject json = null;
                try {
                    json = new JSONObject(response.body().string());
                    double price = json
                            .getJSONObject(coingeckoId)
                            .getDouble("usd");

                    callback.onPrice(price);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

            }

            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e);
            }
        });
    }

    public interface PriceCallback {
        void onPrice(double usdPrice);
        void onError(Exception e);
    }
}

