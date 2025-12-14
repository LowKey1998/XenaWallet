package com.jose.walletapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.jose.walletapp.helpers.MultiChainWalletManager;

import g.p.smartcalculater.R;

public class SignupActivity extends AppCompatActivity {
    FirebaseAuth fa;
    EditText email, password;
    Button signin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(g.p.smartcalculater.R.layout.activity_signup);
        fa=FirebaseAuth.getInstance();
        signin = findViewById(R.id.btnSignUp);
        email=(EditText)findViewById(R.id.emailInput);
        password=(EditText)findViewById(R.id.passwordInput);

        signin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(email.getText().toString().trim().contains("@") && !password.getText().toString().trim().isEmpty()) {
                    fa.createUserWithEmailAndPassword(email.getText().toString(), password.getText().toString()).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // FirebaseDatabase.getInstance().getReference(FirebaseAuth.getInstance().getCurrentUser().getUid() + "/Events").setValue(new ArrayList<String>(Arrays.asList(" ")));
                                FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
                                user.sendEmailVerification().addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Toast.makeText(getApplicationContext(), "we've sent an email verification link to the email account", Toast.LENGTH_SHORT).show();
                                    }
                                });

                                createWallet(user);
                                Toast.makeText(getApplicationContext(), "success", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(SignupActivity.this, MyWalletActivity.class));
                            }
                            else {
                                Toast.makeText(getApplicationContext(), "failed", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
                else{
                    //Toast.makeText(getApplicationContext(), "Check the details you entered, and only northrise university students can register", Toast.LENGTH_SHORT).show();
                }
            }//onClick
        });

    }

    private void createWallet(FirebaseUser user) {
        try {
            MultiChainWalletManager.getInstance().initialize(this, () -> {
                String eth = MultiChainWalletManager.getInstance().getEthAddress();
                String sol = MultiChainWalletManager.getInstance().getSolanaAddress();
                String bsc = MultiChainWalletManager.getInstance().getBscAddress();

                Log.d("WALLET", "ETH: " + eth);
                Log.d("WALLET", "SOL: " + sol);
                Log.d("WALLET", "BSC: " + bsc);
            }, () -> {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(SignupActivity.this, "Wallet initialization failed", Toast.LENGTH_SHORT).show();
                        Log.e("WALLET", "Wallet initialization failed");

                    }
                });
                });
        } catch (Exception e) {
            Toast.makeText(this, "Wallet creation failed", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            Log.e("Wallet", "Wallet creation failed: " + e.getMessage());
        }
    }

}
