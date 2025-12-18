package com.jose.walletapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jose.walletapp.constants.Networks;
import com.jose.walletapp.helpers.HdWalletHelper;
import com.jose.walletapp.helpers.MultiChainWalletManager;
import com.jose.walletapp.helpers.PriceService;
import com.jose.walletapp.helpers.solana.SolTokenOperations;

import org.json.JSONObject;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;

import g.p.smartcalculater.R;

public class SendCryptoActivity extends Activity {
    private static Context context;
    EditText amountET, recipientET;
    private String chain="",contractAddress="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context=this;
        setContentView(R.layout.activity_send_crypto);

        chain = getIntent().getStringExtra("chain");
        contractAddress = getIntent().getStringExtra("contractAddress");


        amountET=findViewById(R.id.etAmount);
        recipientET=findViewById(R.id.etAddress);

        findViewById(R.id.btnCheckFees).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(chain.equalsIgnoreCase(Networks.SOLANA)) {
                    checkSolanaFees();
                } else if (chain.equalsIgnoreCase(Networks.BSC)) {
                    final String recipientAddrStr=recipientET.getText().toString().trim();
                    final String amountStr=amountET.getText().toString();

                    checkBscFees(MultiChainWalletManager.getInstance().getBscAddress(), recipientAddrStr,amountStr);
                }
            }
        });


    }


    private void checkSolanaFees() {
        final BigDecimal LAMPORTS_PER_SOL =
                new BigDecimal("1000000000");

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Typical Solana transfer fee
                BigDecimal feeLamports = new BigDecimal("5000");

                BigDecimal feeSol = feeLamports.divide(
                        LAMPORTS_PER_SOL, 9, RoundingMode.DOWN
                );

                getSolUsdPrice(new PriceService.PriceCallback() {
                    @Override
                    public void onPrice(double usdPrice) {
                        runOnUiThread(() -> showSolanaFee(feeSol, BigDecimal.valueOf(usdPrice)));
                    }

                    @Override
                    public void onError(Exception e) {

                    }
                });


            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this,
                                "Solana fee check failed",
                                Toast.LENGTH_SHORT).show()
                );
            }
        });
    }


    private void getBnbUsdPrice(PriceService.PriceCallback callback) {

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                URL url = new URL(
                        "https://api.coingecko.com/api/v3/simple/price" +
                                "?ids=binancecoin&vs_currencies=usd"
                );

                HttpURLConnection conn =
                        (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("GET");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream())
                );

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();
                conn.disconnect();

                JSONObject json = new JSONObject(response.toString());
                BigDecimal bnbUsd = new BigDecimal(
                        json.getJSONObject("binancecoin")
                                .getDouble("usd")
                );

                runOnUiThread(() -> callback.onPrice(bnbUsd.doubleValue()));

            } catch (Exception e) {
                //runOnUiThread(callback::onError);
            }
        });
    }

    private void checkBscFees(
            String fromAddress,
            String toAddress,
            BigDecimal amountBNB
    ) {

        Executors.newSingleThreadExecutor().execute(() -> {
            try {

                Web3j web3j = Web3j.build(
                        new HttpService("https://bsc-dataseed.binance.org/")
                );

                BigInteger valueWei = Convert.toWei(
                        amountBNB, Convert.Unit.ETHER
                ).toBigInteger();

                EthGasPrice gasPriceResp = web3j.ethGasPrice().send();
                BigInteger gasPrice = gasPriceResp.getGasPrice();

                Transaction tx = Transaction.createEtherTransaction(
                        fromAddress,
                        null,
                        gasPrice,
                        BigInteger.valueOf(21000),
                        toAddress,
                        valueWei
                );

                EthEstimateGas estimateGas =
                        web3j.ethEstimateGas(tx).send();

                BigInteger gasLimit = estimateGas.getAmountUsed();

                BigInteger feeWei = gasLimit.multiply(gasPrice);

                BigDecimal feeBNB = Convert.fromWei(
                        new BigDecimal(feeWei),
                        Convert.Unit.ETHER
                );

                getBnbUsdPrice(new PriceService.PriceCallback() {
                    @Override
                    public void onPrice(double usdPrice) {
                        runOnUiThread(() -> showBscFee(feeBNB, BigDecimal.valueOf(usdPrice)));
                    }

                    @Override
                    public void onError(Exception e) {

                    }
                });


            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this,
                                "BSC fee check failed",
                                Toast.LENGTH_SHORT).show()
                );
            }
        });
    }


    private void showBscFee(
            BigDecimal feeBNB,
            BigDecimal bnbUsdPrice
    ) {

        BigDecimal feeUsd = feeBNB.multiply(bnbUsdPrice)
                .setScale(6, RoundingMode.HALF_UP);

        TextView feeText = findViewById(R.id.feeText);
        LinearLayout layoutFees = findViewById(R.id.layoutFees);

        feeText.setText(
                feeBNB.toPlainString() +
                        " BNB (~$" + feeUsd.toPlainString() + ")"
        );

        layoutFees.setVisibility(View.VISIBLE);
    }


    private void getSolUsdPrice(PriceService.PriceCallback callback) {

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                URL url = new URL(
                        "https://api.coingecko.com/api/v3/simple/price" +
                                "?ids=solana&vs_currencies=usd"
                );

                HttpURLConnection connection =
                        (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);

                InputStream is = connection.getInputStream();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(is)
                );

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();
                connection.disconnect();

                JSONObject json = new JSONObject(response.toString());
                BigDecimal solUsd = new BigDecimal(
                        json.getJSONObject("solana")
                                .getDouble("usd")
                );

                runOnUiThread(() -> callback.onPrice(solUsd.doubleValue()));

            } catch (Exception e) {
                //runOnUiThread(callback.onError);
            }
        });
    }


    private void showSolanaFee(
            BigDecimal feeSol,
            BigDecimal solUsdPrice
    ) {

        BigDecimal feeUsd = feeSol.multiply(solUsdPrice)
                .setScale(6, RoundingMode.HALF_UP);

        TextView feeText = findViewById(R.id.feeText);
        LinearLayout layoutFees = findViewById(R.id.layoutFees);

        feeText.setText(
                feeSol.toPlainString() + " SOL (~$" + feeUsd.toPlainString() + ")"
        );

        layoutFees.setVisibility(View.VISIBLE);
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        /*if(resultCode==1){
            startActivity(new Intent(this,Login.class));
        }
        else{
            finish();
        }*/
    }


}
