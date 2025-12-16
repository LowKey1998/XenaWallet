package com.jose.walletapp;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;

import g.p.smartcalculater.R;

public class TokenListActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_token);
        
    }
}
