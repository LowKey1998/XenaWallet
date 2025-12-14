package com.jose.walletapp.helpers;

import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;

public class OnrampMobileMoney {

    private static final String API_URL = "https://api.onramp.money/v1/transaction/create";
    private static final String API_KEY = "YOUR_API_KEY"; // Replace with your actual API key

    public void initiateMobileMoneyTransaction() {
        OkHttpClient client = new OkHttpClient();

        // Construct the JSON payload
        JSONObject jsonPayload = new JSONObject();
        try {
            jsonPayload.put("appId", "YOUR_APP_ID"); // Replace with your App ID
            jsonPayload.put("walletAddress", "0xYourWalletAddress"); // Replace with user's wallet address
            jsonPayload.put("fiatCurrency", "ZMW"); // Zambian Kwacha
            jsonPayload.put("cryptoCurrency", "USDT"); // Desired cryptocurrency
            jsonPayload.put("blockchain_network", "Solana"); // Desired network
            jsonPayload.put("fiatAmount", 100); // Amount in fiat currency
            jsonPayload.put("paymentMethod", "mobile_money"); // Payment method
            jsonPayload.put("mobileMoneyProvider", "airtel"); // Mobile money provider (e.g., airtel, mtn)
            jsonPayload.put("phoneNumber", "+260971234567"); // User's mobile number

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // Create the request body
        RequestBody body = RequestBody.create(
                jsonPayload.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        // Build the HTTP request
        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .post(body)
                .build();

        // Execute the request asynchronously
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Handle request failure
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    // Handle successful response
                    String responseData = response.body().string();
                    System.out.println("Transaction Successful: " + responseData);
                } else {
                    // Handle unsuccessful response
                    System.out.println("Transaction Failed: " + response.code());
                }
            }
        });
    }
}

