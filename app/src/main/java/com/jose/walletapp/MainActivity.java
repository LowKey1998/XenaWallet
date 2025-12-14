package com.jose.walletapp;


import static com.jose.walletapp.helpers.MnemonicGenerator.saveMnemonic;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;


import com.jose.walletapp.helpers.ECKeyStorage;
import com.jose.walletapp.helpers.HdWalletHelper;
import com.jose.walletapp.helpers.MnemonicGenerator;

import org.json.JSONObject;

import java.io.IOException;

import g.p.smartcalculater.R;
import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


class BasicAuthInterceptor implements Interceptor {

    private String credentials;

    public BasicAuthInterceptor(String user, String password) {
        this.credentials = Credentials.basic(user, password);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Request authenticatedRequest = request.newBuilder()
                .header("Authorization", credentials).build();
        return chain.proceed(authenticatedRequest);
    }

}


public class MainActivity extends Activity {
    OkHttpClient client = new OkHttpClient.Builder().addInterceptor(new BasicAuthInterceptor("u0ldmyg3ki",
            "hBF-9l2g-NceVLue05pjfrNop46Qr5JktEeMqsHlJ94")).build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);
        findViewById(R.id.create).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*try {
                    new Thread(){
                        @Override
                        public void run() {

                            try {
                                String response=post("https://u0d7x309az-u0skh4qws8-hdwallet.us0-aws.kaleido.io/api/v1/wallets","");
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {

                                        try {
                                            System.out.println(response);
                                            Intent intent=new Intent(getApplicationContext(),SeedPhraseActivity.class);
                                            JSONObject responseJson=new JSONObject(response);
                                            //intent.putExtra("secret", responseJson.getString("secret"));
                                            //ToDo:store phrae in firebase for easy login to wallet using user email or phone

                                            //create wallet
                                            String mnemonic = MnemonicGenerator.generateMnemonic();//responseJson.getString("secret");
                                            HdWalletHelper.createWalletFromMnemonic(mnemonic);

                                            //MainActivity.this.startActivity(intent);
                                            *//*Toast.makeText(getApplicationContext(),response
                                                    ,Toast.LENGTH_SHORT).show();*//*
                                        } catch (Exception e) {

                                            Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                            }


                        }
                    }.start();

                } catch (Exception e) {

                }*/

                //create wallet
                String mnemonic = null;//responseJson.getString("secret");
                try {
                    mnemonic = MnemonicGenerator.generateMnemonic();
                    saveMnemonic(MainActivity.this,mnemonic);
                    HdWalletHelper.createWalletFromMnemonic(MainActivity.this,mnemonic);
                    if(ECKeyStorage.loadPrivateKey(MainActivity.this)!=null){
                        Intent intent=new Intent(getApplicationContext(),MyWalletActivity.class);
                        intent.putExtra("secret", mnemonic);
                        //ToDo:store phrae in firebase for easy login to wallet using user email or phone


                        MainActivity.this.startActivity(intent);
                    }
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_SHORT).show();
                }

            }
        });
        findViewById(R.id.login).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
            }
        });

    }

    public static final MediaType JSON = MediaType.get("application/json");
    String post(String url, String json) throws IOException {

        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        }
    }
}
