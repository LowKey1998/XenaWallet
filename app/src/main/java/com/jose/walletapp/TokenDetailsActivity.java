package com.jose.walletapp;

import static com.jose.walletapp.ERC20Metadata.callStringFunction;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.jose.walletapp.constants.Networks;

import org.json.JSONArray;
import org.json.JSONObject;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import g.p.smartcalculater.R;

public class TokenDetailsActivity extends Activity {

    private TextView title, balanceZMW, balanceSOL;
    private ImageView tokenLogo, qrCodeView;
    private LineChart priceChart;
    private ListView transactionList;

    private String chainId;
    private String contractAddress;
    private String userWalletAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_wallet);

        // UI elements
        title = findViewById(R.id.title);
        balanceZMW = findViewById(R.id.balance_zmw);
        balanceSOL = findViewById(R.id.balance_sol);
        transactionList = findViewById(R.id.transaction_list);

        // Suppose your XML has ImageView with id token_logo and qr_code
       // tokenLogo = findViewById(R.id.token_logo);
        qrCodeView = findViewById(R.id.qr_code);
        //priceChart = findViewById(R.id.price_chart);

        //todo remove
        generateQRCode(userWalletAddress);

        // Get intent extras
        chainId = getIntent().getStringExtra("chain");
        contractAddress = getIntent().getStringExtra("contractAddress");

        // Fetch token details
        fetchTokenDetails();
    }

    private void fetchTokenDetails() {
        if(chainId.equalsIgnoreCase(Networks.BSC)) {
            //fetchBscTokenName(contractAddress); // Or your blockchain-specific checker
            Web3j web3j = Web3j.build(new HttpService("https://bsc-dataseed.binance.org/"));

            try {
                String name = callStringFunction(web3j, contractAddress, "name");
                String tokenSymbol = callStringFunction(web3j, contractAddress, "symbol");
                //int decimals = callUint8Function(web3j, contractAddress, "decimals");

                //runOnUiThread(() ->{
                title.setText(name);
                //symbolEditText.setText(tokenSymbol);
                //});
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        else if(chainId.equalsIgnoreCase(Networks.SOLANA)){
            //fetchSolanaTokenMetadata(contractAddress);
        }
    }

    private void displayToken(TokenData tokenData) {
        title.setText(tokenData.name);
        balanceZMW.setText("ZMW " + tokenData.balance.toPlainString());

        // Display QR code for user wallet
        generateQRCode(userWalletAddress);

        // Load logo (use Glide/Picasso)
        //Glide.with(this).load(tokenData.logoUrl).into(tokenLogo);

        // Draw price chart (dummy example)
        drawChart(tokenData);

        // Populate transaction list (if you have transaction fetching)
        // transactionList.setAdapter(new TransactionAdapter(...));
    }

    private void generateQRCode(String content) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            Bitmap bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565);
            com.google.zxing.common.BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512);
            for (int x = 0; x < 512; x++) {
                for (int y = 0; y < 512; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            qrCodeView.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    private void drawChart(TokenData tokenData) {
        /*List<Entry> entries = new ArrayList<>();
        // Dummy data for illustration
        entries.add(new Entry(0, tokenData.balance.floatValue()));
        entries.add(new Entry(1, tokenData.balance.floatValue().multiply(new BigDecimal("1.05")).floatValue()));
        entries.add(new Entry(2, tokenData.balance.floatValue().multiply(new BigDecimal("1.1")).floatValue()));

        LineDataSet dataSet = new LineDataSet(entries, tokenData.symbol + " Price");
        LineData lineData = new LineData(dataSet);
        priceChart.setData(lineData);
        priceChart.invalidate();*/ // refresh
    }

    private String getCoinGeckoTokenApi(String chainId, String contractAddress) {
        // Map chainId to CoinGecko network slug
        String network = "ethereum";
        if (chainId.equalsIgnoreCase("bsc")) network = "binance-smart-chain";
        else if (chainId.equalsIgnoreCase("sol")) network = "solana";

        return "https://api.coingecko.com/api/v3/onchain/networks/" + network + "/tokens/" + contractAddress;
    }

    // Simple token data holder
    private static class TokenData {
        String symbol;
        String name;
        String logoUrl;
        BigDecimal balance;
        String coingeckoId;
        int decimals;

        TokenData(String symbol, String name, String logoUrl, BigDecimal balance, String coingeckoId, int decimals) {
            this.symbol = symbol;
            this.name = name;
            this.logoUrl = logoUrl;
            this.balance = balance;
            this.coingeckoId = coingeckoId;
            this.decimals = decimals;
        }
    }
}
