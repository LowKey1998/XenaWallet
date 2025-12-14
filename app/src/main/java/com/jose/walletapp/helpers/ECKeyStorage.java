package com.jose.walletapp.helpers;

import android.content.Context;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.math.BigInteger;

public class ECKeyStorage {

    private static final String PREF_NAME = "secure_keys";
    private static final String PRIVATE_KEY_KEY = "ec_private_key";

    public static void savePrivateKey(Context context, BigInteger privateKey)
            throws GeneralSecurityException, IOException {

        String masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);

        var prefs = EncryptedSharedPreferences.create(
                PREF_NAME,
                masterKey,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );

        prefs.edit().putString(PRIVATE_KEY_KEY, privateKey.toString(16)).apply();
    }

    public static BigInteger loadPrivateKey(Context context)
            throws GeneralSecurityException, IOException {

        String masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);

        var prefs = EncryptedSharedPreferences.create(
                PREF_NAME,
                masterKey,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );

        String hex = prefs.getString(PRIVATE_KEY_KEY, null);
        return hex != null ? new BigInteger(hex, 16) : null;
    }
}

