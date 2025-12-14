package com.jose.walletapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.flexbox.FlexboxLayout;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.jose.walletapp.helpers.HdWalletHelper;
import com.jose.walletapp.helpers.MultiChainWalletManager;

import org.web3j.crypto.Credentials;

import g.p.smartcalculater.R;

public class LoginActivity extends Activity {
    FirebaseAuth fa;
    EditText email=null;
    EditText password=null;
    private EditText[] wordInputs = new EditText[12];
    private LinearLayout container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        //FlexboxLayout mnemonicContainer = findViewById(R.id.mnemonicContainer);
        /*for (int i = 0; i < 12; i++) {
            EditText input = new EditText(this);
            input.setId(View.generateViewId());
            input.setHint("Word " + (i + 1));
            input.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            input.setLayoutParams(new FlexboxLayout.LayoutParams(
                    getResources().getDisplayMetrics().widthPixels / 3 - 32,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            input.setPadding(16, 16, 16, 16);
            input.setTextSize(16);
            input.setBackgroundResource(R.drawable.edittext_background);
            wordInputs[i] = input;
            mnemonicContainer.addView(input);

            final int currentIndex = i;
            input.addTextChangedListener(new TextWatcher() {
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() > 0 && currentIndex < 11) {
                        wordInputs[currentIndex + 1].requestFocus();
                    }
                }

                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                public void afterTextChanged(Editable s) {}
            });

            input.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL &&
                        event.getAction() == KeyEvent.ACTION_DOWN &&
                        input.getText().toString().isEmpty() &&
                        currentIndex > 0) {
                    wordInputs[currentIndex - 1].requestFocus();
                    return true;
                }
                return false;
            });
        }*/

        if(FirebaseAuth.getInstance().getCurrentUser()!=null){
            startActivity(new Intent(this,MainActivity.class));
            finish();
        }
        email=findViewById(R.id.emailInput);
        password=findViewById(R.id.passwordInput);
        fa=FirebaseAuth.getInstance();

        /*findViewById(R.id.signUpText).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, SignupActivity.class));
            }
        });*/
        findViewById(R.id.loginBtn).setOnClickListener(v -> {
            /*StringBuilder phrase = new StringBuilder();
            for (EditText e : wordInputs) {
                String word = e.getText().toString().trim();
                if (word.isEmpty()) {
                    Toast.makeText(this, "All 12 words are required", Toast.LENGTH_SHORT).show();
                    return;
                }
                phrase.append(word).append(" ");
            }
            String finalPhrase = phrase.toString().trim();
            Log.d("MNEMONIC", "Phrase: " + finalPhrase);*/

            // TODO: Handle login with phrase
                /*try {
                    MultiChainWalletManager.getInstance().initialize(this, () -> {
                        String eth = MultiChainWalletManager.getInstance().getEthAddress();
                        String sol = MultiChainWalletManager.getInstance().getSolanaAddress();
                        String bsc = MultiChainWalletManager.getInstance().getBscAddress();

                        Log.d("WALLET", "ETH: " + eth);
                        Log.d("WALLET", "SOL: " + sol);
                        Log.d("WALLET", "BSC: " + bsc);
                    }, () -> {
                        Toast.makeText(this, "Wallet initialization failed", Toast.LENGTH_SHORT).show();
                        Log.e("WALLET", "Wallet initialization failed");
                    });
                } catch (Exception e) {
                    Toast.makeText(this, "Wallet creation failed", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                    Log.e("Wallet", "Wallet creation failed: " + e.getMessage());
                }*/
            loginClicked();

        });
    }


    public void loginClicked(){
        final String emailString=email.getText().toString();
        if (!emailString.trim().isEmpty() && !password.getText().toString().trim().isEmpty()) {
            //if some requirements met create a signed in user
            fa.signInWithEmailAndPassword(emailString, password.getText().toString()).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        getPreferences(0).edit().putString("email",emailString).commit();
                        Toast.makeText(getApplicationContext(), "success", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, MyWalletActivity.class));
                    } else {
                        Toast.makeText(getApplicationContext(), "failed", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
        else{
            Toast.makeText(getApplicationContext(),"please fill in email and password(8 characters long)",Toast.LENGTH_SHORT).show();
        }
    }
}

