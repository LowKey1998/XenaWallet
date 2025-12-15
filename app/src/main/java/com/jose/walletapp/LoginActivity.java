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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

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

