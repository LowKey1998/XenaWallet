package com.jose.walletapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.jose.walletapp.helpers.HdWalletHelper;

import g.p.smartcalculater.R;

public class SendCryptoActivity extends Activity {
    private static Context context;
    EditText amountET, recipientET;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context=this;

        setContentView(R.layout.activity_send_crypto);

        amountET=findViewById(R.id.amount_input);
        //recipientET=findViewById(R.id.editTextRecipient);

        findViewById(R.id.next_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String amount=amountET.getText().toString().trim();
                String recipient=recipientET.getText().toString().trim();
                HdWalletHelper.sendUsdt(context,recipient,Float.parseFloat(amount));
            }
        });



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
