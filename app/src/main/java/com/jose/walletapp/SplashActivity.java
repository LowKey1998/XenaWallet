package com.jose.walletapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.firebase.auth.FirebaseAuth;
import com.jose.walletapp.helpers.MnemonicGenerator;

import g.p.smartcalculater.R;


public class SplashActivity extends Activity {
    private static Context context;

    private static boolean isNightModeEnabled = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context=this;

        setContentView(R.layout.activity_splash_screen);

        Thread background = new Thread() {
            public void run() {
                try {
                    sleep(1800);

                    if(/*MnemonicGenerator.getMnemonic(SplashActivity.this)*/FirebaseAuth.getInstance().getCurrentUser() !=null){
                        Intent intent = new Intent(SplashActivity.this, MyWalletActivity.class);
                        startActivityForResult(intent, 2);
                    }
                    else {
                        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                        startActivityForResult(intent, 2);
                    }

                } catch (Exception e) {

                }
            }
        };

        // start thread
        background.start();

    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode==1){
            startActivity(new Intent(this,LoginActivity.class));
        }
        else{
            finish();
        }
    }


}
