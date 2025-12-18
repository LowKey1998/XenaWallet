package com.jose.walletapp;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.jose.walletapp.constants.Networks;
import com.jose.walletapp.helpers.MultiChainWalletManager;
import com.jose.walletapp.helpers.Token;
import com.jose.walletapp.helpers.bsc.BscHelper;
import com.jose.walletapp.helpers.coingecko.CoinGeckoTokenHelper;
import com.jose.walletapp.helpers.solana.SolTokenOperations;
import com.jose.walletapp.helpers.solana.SolanaHelper;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;

import g.p.smartcalculater.R;

public class ReceiveActivity extends Activity {

    ImageView backButton, qrCodeImage;
    TextView titleText, symbolText,balanceText, walletAddressText;
    Button copyButton;
    private String walletAddress="";
    private String chain, contractAddress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_token);
        initViews();

        // Get intent extras
        chain = getIntent().getStringExtra("chain");
        contractAddress = getIntent().getStringExtra("contractAddress");
        if(chain.equalsIgnoreCase(Networks.SOLANA)) {
            walletAddress = MultiChainWalletManager.getInstance().getSolanaAddress();
        } else if (chain.equalsIgnoreCase(Networks.BSC)) {
            walletAddress = MultiChainWalletManager.getInstance().getBscAddress();
        }
        setupUI();
        generateQRCode(walletAddress);
        setupClicks();

    }

    private void initViews() {
        backButton = findViewById(R.id.back_button);
        qrCodeImage = findViewById(R.id.qr_code);
        titleText = findViewById(R.id.receive_token_title);
        walletAddressText = findViewById(R.id.wallet_address);
        copyButton = findViewById(R.id.copy_button);
       // symbolText = findViewById(R.id.symbol);
        balanceText = findViewById(R.id.token_balance);
    }

    private void setupUI() {
        // Example token name

        try {
            new Thread(){
                @Override
                public void run() {
                    JSONObject tokenInfo = null;
                    if(chain.equalsIgnoreCase(Networks.SOLANA)) {
                        //fetch details
                        try {
                            String coinGeckoFetchChainId = CoinGeckoTokenHelper.coinIdSolana;
                            CoinGeckoTokenHelper.fetchTokenInfo(coinGeckoFetchChainId, contractAddress, chain, new CoinGeckoTokenHelper.TokenCallback() {
                                @Override
                                public void onSuccess(Token token, Set<String> platforms) {
                                    //String tokenName = tokenInfo.getString("name");
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            titleText.setText("Receive " + token.name);
                                           // symbolText.setText(token.symbol);


                                        }
                                    });
                                }

                                @Override
                                public void onError(String error) {

                                }
                            });

                        }
                        catch (Exception e){

                        }

                        //fetch balance
                        try {
                            double balance=SolTokenOperations.getUserSplTokenBalance(walletAddress, contractAddress);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    balanceText.setText("USD "+balance);
                                }
                            });
                        }
                        catch (Exception e){

                        }
                    }
                    else if(chain.equalsIgnoreCase(Networks.BSC)){
                        tokenInfo= new BscHelper().getTokenInfo(contractAddress);
                        //String tokenName = tokenInfo.getString("name");
                        JSONObject finalTokenInfo = tokenInfo;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    titleText.setText("Receive "+ finalTokenInfo.getString("name"));
                                } catch (JSONException e) {
                                    //throw new RuntimeException(e);
                                }
                            }
                        });
                    }


                }
            }.start();
        } catch (Exception e) {
           // throw new RuntimeException(e);
        }

        walletAddressText.setText(walletAddress);
    }

    private void setupClicks() {

        backButton.setOnClickListener(v -> finish());

        copyButton.setOnClickListener(v -> {
            ClipboardManager clipboard =
                    (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Wallet Address", walletAddress);
            clipboard.setPrimaryClip(clip);

            //Toast.makeText(this, "Address copied", Toast.LENGTH_SHORT).show();
        });
    }

    private void generateQRCode(String text) {
        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(
                    text,
                    BarcodeFormat.QR_CODE,
                    600,
                    600
            );
            qrCodeImage.setImageBitmap(bitmap);

        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(this, "QR generation failed", Toast.LENGTH_SHORT).show();
        }
    }
}
