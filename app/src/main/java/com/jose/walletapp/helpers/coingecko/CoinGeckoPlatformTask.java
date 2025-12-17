package com.jose.walletapp.helpers.coingecko;

import android.os.AsyncTask;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class CoinGeckoPlatformTask extends AsyncTask<Void, Void, Set<String>> {

    private final String coinId;
    private final Callback callback;

    public interface Callback {
        void onResult(Set<String> platforms);
    }

    public CoinGeckoPlatformTask(String coinId, Callback callback) {
        this.coinId = coinId;
        this.callback = callback;
    }

    @Override
    protected Set<String> doInBackground(Void... voids) {
        Set<String> platforms = new HashSet<>();
        try {
            URL url = new URL(
                    "https://api.coingecko.com/api/v3/coins/" + coinId +
                            "?localization=false&tickers=false&market_data=false" +
                            "&community_data=false&developer_data=false"
            );

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
            );

            StringBuilder json = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) json.append(line);

            JSONObject root = new JSONObject(json.toString());
            JSONObject plats = root.getJSONObject("platforms");

            Iterator<String> keys = plats.keys();
            while (keys.hasNext()) {
                platforms.add(keys.next());
            }

        } catch (Exception ignored) {}
        return platforms;
    }

    @Override
    protected void onPostExecute(Set<String> result) {
        callback.onResult(result);
    }
}

