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

