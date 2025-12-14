package com.jose.walletapp.helpers;


import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.DeterministicHierarchy;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.p2p.solanaj.core.Account;
import org.web3j.crypto.Bip32ECKeyPair;
import org.web3j.crypto.MnemonicUtils;
import org.web3j.crypto.WalletUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class MultiChainWalletManager {
    private static MultiChainWalletManager instance;
    private String mnemonic;
    private String ethAddress;
    private String bscAddress;
    private String solanaAddress;

    private static final String NODE = "wallets";

    public static MultiChainWalletManager getInstance() {
        if (instance == null) {
            instance = new MultiChainWalletManager();
        }
        return instance;
    }

    public void initialize(Context context, Runnable onReady, Runnable onFail) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            onFail.run();
            return;
        }

        user.getIdToken(true).addOnSuccessListener(result -> {
            String uid = user.getUid();
            String token = result.getToken();
            try {
                byte[] aesKey = sha256(uid);//sha256(uid + token);
                loadOrCreateWallet(uid, aesKey, onReady, onFail);
            } catch (Exception e) {
                e.printStackTrace();
                onFail.run();
            }
        }).addOnFailureListener(e -> onFail.run());
    }

    private void loadOrCreateWallet(String uid, byte[] aesKey, Runnable onReady, Runnable onFail) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(NODE).child(uid);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.child("encryptedMnemonic").exists()) {
                    String encrypted = snapshot.child("encryptedMnemonic").getValue(String.class);
                    try {
                        mnemonic = decrypt(encrypted, aesKey);
                        deriveWallets();
                        onReady.run();
                    } catch (Exception e) {
                        e.printStackTrace();
                        onFail.run();
                    }
                } else {
                    try {
                        mnemonic = generateMnemonic();
                        String encrypted = encrypt(mnemonic, aesKey);

                        Map<String, Object> data = new HashMap<>();
                        data.put("encryptedMnemonic", encrypted);

                        dbRef.setValue(data).addOnSuccessListener(aVoid -> {
                            try {
                                deriveWallets();
                            } catch (MnemonicException.MnemonicLengthException e) {
                                throw new RuntimeException(e);
                            }
                            onReady.run();
                        }).addOnFailureListener(e -> onFail.run());
                    } catch (Exception e) {
                        e.printStackTrace();
                        onFail.run();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                onFail.run();
            }
        });
    }

    private void deriveWallets() throws MnemonicException.MnemonicLengthException {
        // Ethereum / BSC
        byte[] seed = MnemonicUtils.generateSeed(mnemonic, "");
        Bip32ECKeyPair masterKeypair = Bip32ECKeyPair.generateKeyPair(seed);
        int[] path = {44 | 0x80000000, 60 | 0x80000000, 0 | 0x80000000, 0, 0};
        Bip32ECKeyPair bip44Keypair = Bip32ECKeyPair.deriveKeyPair(masterKeypair, path);
        //ethAddress = WalletUtils.getAddress(bip44Keypair);
        ethAddress = "0x" + org.web3j.crypto.Keys.getAddress(bip44Keypair.getPublicKey());

        bscAddress = ethAddress; // Same derivation

        // Solana
        DeterministicSeed ds = null;
        try {
            ds = new DeterministicSeed(mnemonic, null, "", 0);
        } catch (UnreadableWalletException e) {
            //todo:maybe remove
            throw new RuntimeException(e);
        }
        byte[] solanaSeed = ds.getSeedBytes();
        Account solanaAccount = new Account(solanaSeed);
        solanaAddress = solanaAccount.getPublicKey().toBase58();
    }

    private String generateMnemonic() throws Exception {
        byte[] entropy = new byte[16];
        new SecureRandom().nextBytes(entropy);
        List<String> words = MnemonicCode.INSTANCE.toMnemonic(entropy);
        return String.join(" ", words);
    }

    private String encrypt(String plaintext, byte[] key) throws Exception {
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

        byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
        return Base64.encodeToString(combined, Base64.NO_WRAP);
    }

    private String decrypt(String ciphertext, byte[] key) throws Exception {
        byte[] combined = Base64.decode(ciphertext, Base64.NO_WRAP);
        byte[] iv = Arrays.copyOfRange(combined, 0, 12);
        byte[] encrypted = Arrays.copyOfRange(combined, 12, combined.length);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

        byte[] decrypted = cipher.doFinal(encrypted);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    private byte[] sha256(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(input.getBytes(StandardCharsets.UTF_8));
    }

    public String getMnemonic() { return mnemonic; }
    public String getEthAddress() { return ethAddress; }
    public String getBscAddress() { return bscAddress; }
    public String getSolanaAddress() { return solanaAddress; }
}

