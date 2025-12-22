package com.jose.walletapp;

import static com.jose.walletapp.helpers.ERC20Metadata.callStringFunction;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.jose.walletapp.constants.Networks;

import org.json.JSONObject;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import g.p.smartcalculater.R;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AddTokenActivity extends Activity {
    private Button fetchTokenDetailsButton;
    private EditText contractAddessEditText;
    private String lastCheckedAddress = "";
    private EditText nameEditText, symbolEditText, coinGeckoIdEditText;
    private Spinner networkSpinner;
    private String selectedBlockchain;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_token);

        fetchTokenDetailsButton = findViewById(R.id.button_check_add);
        contractAddessEditText = findViewById(R.id.contractAddress);
        nameEditText = findViewById(R.id.name);
        coinGeckoIdEditText = findViewById(R.id.coingecko_id);
        symbolEditText = findViewById(R.id.symbol);
        networkSpinner = findViewById(R.id.network);

        // Set up Spinner with blockchain options
        ArrayAdapter<CharSequence> blockchainAdapter = ArrayAdapter.createFromResource(this,
                R.array.blockchain_options, android.R.layout.simple_spinner_item);
        blockchainAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        networkSpinner.setAdapter(blockchainAdapter);

        // Set listener to get selected blockchain and enable the EditText
        networkSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                selectedBlockchain = parentView.getItemAtPosition(position).toString();

                // Enable the EditText when a blockchain is selected
                if (selectedBlockchain != null && !selectedBlockchain.isEmpty()) {
                    contractAddessEditText.setEnabled(true);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Disable the EditText if no blockchain is selected
                contractAddessEditText.setEnabled(false);
            }
        });

        // Watch for address change
        contractAddessEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String currentText = s.toString().trim();
                if (!currentText.equalsIgnoreCase(lastCheckedAddress)) {
                    fetchTokenDetailsButton.setText("Check");
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });


        fetchTokenDetailsButton.setOnClickListener(v -> {
            String contractAddress = contractAddessEditText.getText().toString().trim();

            if (contractAddress.isEmpty()) {
                Toast.makeText(this, "Please enter a contract address", Toast.LENGTH_SHORT).show();
                return;
            }

            String buttonText = fetchTokenDetailsButton.getText().toString();

            if (buttonText.equalsIgnoreCase("Add")) {
                // ✅ Add token to Firebase
                addTokenToFirebase(contractAddress);

            } else if (buttonText.equalsIgnoreCase("Check")) {
                // ✅ Check contract validity
                if(selectedBlockchain.equalsIgnoreCase(Networks.BSC)) {
                    //fetchBscTokenName(contractAddress); // Or your blockchain-specific checker
                    Web3j web3j = Web3j.build(new HttpService("https://bsc-dataseed.binance.org/"));

                    try {
                        String name = callStringFunction(web3j, contractAddress, "name");
                        String tokenSymbol = callStringFunction(web3j, contractAddress, "symbol");
                        //int decimals = callUint8Function(web3j, contractAddress, "decimals");

                        //runOnUiThread(() ->{
                            nameEditText.setVisibility(View.VISIBLE);
                            nameEditText.setText(name);
                            symbolEditText.setVisibility(View.VISIBLE);
                            symbolEditText.setText(tokenSymbol);
                            fetchTokenDetailsButton.setText("Add");
                            lastCheckedAddress = contractAddress; // save verified address
                        //});
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                else if(selectedBlockchain.equalsIgnoreCase(Networks.SOLANA)){
                    fetchSolanaTokenMetadata(contractAddress);
                }
            }
        });

        
    }//onCreate


    private void addTokenToFirebase(String contractAddress) {
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("ProductionDB/Users/"+ FirebaseAuth.getInstance().getCurrentUser().getUid()+"/Tokens");

        // Create a token object
        Map<String, Object> tokenData = new HashMap<>();
        tokenData.put("contractAddress", contractAddress);
        tokenData.put("chain", selectedBlockchain); // Optional, can also use "solana", "tron", etc.
        tokenData.put("timestamp", System.currentTimeMillis());
        tokenData.put("coingeckoId", coinGeckoIdEditText.getText().toString());

        // Push the data to Firebase
        databaseRef.push().setValue(tokenData)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Token added to Firebase", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to add: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }//addTokenToFirebase


/*
    private void fetchBscTokenName(String contractAddress) {
        new Thread(() -> {
            try {
                Web3j web3j = Web3j.build(new HttpService("https://bsc-dataseed.binance.org/"));

                Function nameFunction = new Function(
                        "name",
                        Collections.emptyList(),
                        Collections.singletonList(new TypeReference<Utf8String>() {})
                );
                Function decimalsFunction = new Function(
                        "decimals",
                        Collections.emptyList(),
                        Collections.singletonList(new TypeReference<Utf8String>() {})
                );
                Function symbolFunction = new Function(
                        "symbol",
                        Collections.emptyList(),
                        Collections.singletonList(new TypeReference<Utf8String>() {})
                );

                String encodedFunction = FunctionEncoder.encode(nameFunction);

                Transaction transaction = Transaction.createEthCallTransaction(
                        "0x0000000000000000000000000000000000000001",
                        contractAddress,
                        encodedFunction
                );

                EthCall response = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).send();
                String raw = response.getValue();

                List<Type> decoded = FunctionReturnDecoder.decode(raw, nameFunction.getOutputParameters());

                if (!decoded.isEmpty()) {
                    String name = decoded.get(0).getValue().toString();

                    runOnUiThread(() -> {
                        // Show toast and change button to "Add"
                        //Toast.makeText(this, "Token Name: " + name, Toast.LENGTH_SHORT).show();
                        nameEditText.setVisibility(View.VISIBLE);
                        nameEditText.setText(name);
                        symbolEditText.setVisibility(View.VISIBLE);
                        symbolEditText.setText("...");
                        fetchTokenDetailsButton.setText("Add");
                        lastCheckedAddress = contractAddress; // save verified address
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Invalid contract or no token name", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Fetch failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
*/


    private void fetchSolanaTokenMetadata(String contractAddress) {
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
                        String logoUrl = jsonResponse.getJSONObject("image").getString("small");
                        String name = jsonResponse.getString("name");
                        String tokenSymbol = jsonResponse.getString("symbol");
                        String coinGeckoId = null;
                        try {
                            coinGeckoId = jsonResponse.getString("id");
                        }
                        catch (Exception e){

                        }


                        // Handle the token logo URL and name
                        String finalCoinGeckoId = coinGeckoId;
                        runOnUiThread(() ->{
                            nameEditText.setVisibility(View.VISIBLE);
                            nameEditText.setText(name);
                            symbolEditText.setVisibility(View.VISIBLE);
                            symbolEditText.setText(tokenSymbol);
                            coinGeckoIdEditText.setText(finalCoinGeckoId);
                            fetchTokenDetailsButton.setText("Add");
                            lastCheckedAddress = contractAddress; // save verified address
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> Toast.makeText(AddTokenActivity.this,
                                "Error parsing token metadata", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(AddTokenActivity.this,
                            "Error fetching token metadata", Toast.LENGTH_SHORT).show());
                }
            }


            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(AddTokenActivity.this,
                        "Failed to fetch token metadata", Toast.LENGTH_SHORT).show());
            }

        });
    }

}//AddTokenActivity
