package com.jose.walletapp.helpers.solana;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.math.BigInteger;

public class SolanaHelper {



    // Fetch SPL token info

    public static JSONObject getTokenInfo(String mintAddress,String apiKey) {
        JSONObject assetInfo = new JSONObject();
        try {
            URL url = new URL("https://mainnet.helius-rpc.com/?api-key=" + apiKey);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // JSON-RPC request
            JSONObject body = new JSONObject();
            body.put("jsonrpc", "2.0");
            body.put("id", 1);
            body.put("method", "getAsset");
            JSONObject params = new JSONObject();
            params.put("asset", mintAddress);
            body.put("params", params);

            OutputStream os = conn.getOutputStream();
            os.write(body.toString().getBytes());
            os.flush();

            Scanner sc = new Scanner(conn.getInputStream());
            StringBuilder response = new StringBuilder();
            while (sc.hasNext()) response.append(sc.nextLine());
            sc.close();

            JSONObject resp = new JSONObject(response.toString());
            if (resp.has("result")) {
                JSONObject data = resp.getJSONObject("result");
                assetInfo.put("name", data.optString("name"));
                assetInfo.put("symbol", data.optString("symbol"));
                assetInfo.put("decimals", data.optInt("decimals"));
                assetInfo.put("logo", data.optString("image")); // sometimes it's "image" or "icon"
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return assetInfo;
    }


    // Send SPL token using Solana JSON RPC
    public static String sendToken(String senderPrivateKey, String toAddress, String tokenAddress, long amount, String rpcUrl) {
        try {
            // Example using Solana RPC sendTransaction endpoint
            URL url = new URL(rpcUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // Build simple JSON RPC request (you will need to sign the transaction offline in real use)
            String inputJson = "{ \"jsonrpc\":\"2.0\", \"id\":1, \"method\":\"sendTransaction\", \"params\":[\"<signed_transaction_base64>\"] }";

            OutputStream os = conn.getOutputStream();
            os.write(inputJson.getBytes());
            os.flush();

            Scanner sc = new Scanner(conn.getInputStream());
            StringBuilder response = new StringBuilder();
            while (sc.hasNext()) response.append(sc.nextLine());
            sc.close();
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

