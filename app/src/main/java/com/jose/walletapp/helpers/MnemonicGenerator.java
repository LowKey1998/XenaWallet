package com.jose.walletapp.helpers;

import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.wallet.DeterministicSeed;

import java.security.SecureRandom;
import java.util.List;

import android.content.Context;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class MnemonicGenerator {
    private static final String PREF_NAME = "secure_prefs";
    private static final String MNEMONIC_KEY = "wallet_mnemonic";
    private static final String PRIVATE_KEY = "wallet_mnemonic";

    public static String generateMnemonic() throws Exception {
        SecureRandom secureRandom = new SecureRandom();
        byte[] entropy = new byte[16]; // 128 bits = 12 words
        secureRandom.nextBytes(entropy);

        List<String> mnemonicWords = MnemonicCode.INSTANCE.toMnemonic(entropy);
        String mnemonic= String.join(" ", mnemonicWords);

        return mnemonic;
    }

    public static void saveMnemonic(Context context, String mnemonic) throws GeneralSecurityException, IOException {
        String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);

        var sharedPreferences = EncryptedSharedPreferences.create(
                PREF_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );

        sharedPreferences.edit().putString(MNEMONIC_KEY, mnemonic).apply();
    }


    public static String getMnemonic(Context context) throws GeneralSecurityException, IOException {
        String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);

        var sharedPreferences = EncryptedSharedPreferences.create(
                PREF_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );

        return sharedPreferences.getString(MNEMONIC_KEY, null);
    }

}

