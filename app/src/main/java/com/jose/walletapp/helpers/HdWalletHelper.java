package com.jose.walletapp.helpers;


import static com.jose.walletapp.constants.Constants.INFURA_KEY;

import android.content.Context;

import android.util.Log;

import org.p2p.solanaj.core.PublicKey;
import org.p2p.solanaj.rpc.RpcClient;
import org.web3j.crypto.*;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicHierarchy;
import org.bitcoinj.crypto.DeterministicKey;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.Transfer;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.utils.Convert;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

import okhttp3.OkHttpClient;

public class HdWalletHelper {

    public static void createWalletFromMnemonic(Context context, String mnemonic) throws Exception {
        // 1. Validate mnemonic
        List<String> mnemonicWords = List.of(mnemonic.split(" "));
        MnemonicCode.INSTANCE.check(mnemonicWords);

        // 2. Generate seed from mnemonic (empty password)
        byte[] seed = MnemonicCode.toSeed(mnemonicWords, "");

        // 3. Create master key
        DeterministicKey masterKey = HDKeyDerivation.createMasterPrivateKey(seed);
        DeterministicHierarchy dh = new DeterministicHierarchy(masterKey);

        // 4. Derive child key using BIP44 path: m/44'/60'/0'/0/0
        List<ChildNumber> bip44Path = List.of(
                new ChildNumber(44, true),
                new ChildNumber(60, true),
                new ChildNumber(0, true),
                ChildNumber.ZERO,
                ChildNumber.ZERO
        );

        DeterministicKey childKey = dh.get(bip44Path, true,true);
        ECKeyPair ecKeyPair = ECKeyPair.create(childKey.getPrivKeyBytes());



        // 5. Get credentials and address
        Credentials credentials = Credentials.create(ecKeyPair);
        System.out.println("Address: " + credentials.getAddress());
        System.out.println("Private Key: " + credentials.getEcKeyPair().getPrivateKey().toString(16));
        //save private key
        ECKeyStorage.savePrivateKey(context,credentials.getEcKeyPair().getPrivateKey());
    }

    public static Credentials loginFromMnemonic(Context context,String mnemonic) throws Exception {
        // Validate mnemonic
        List<String> words = Arrays.asList(mnemonic.trim().split(" "));
        MnemonicCode.INSTANCE.check(words);

        // Generate seed from mnemonic
        byte[] seed = MnemonicCode.toSeed(words, "");

        // Derive master key
        Bip32ECKeyPair masterKeypair = Bip32ECKeyPair.generateKeyPair(seed);

        // Derivation path for Ethereum/Polygon/Avalanche
        final int[] derivationPath = {44 | 0x80000000, 60 | 0x80000000, 0 | 0x80000000, 0, 0};
        Bip32ECKeyPair derivedKeyPair = Bip32ECKeyPair.deriveKeyPair(masterKeypair, derivationPath);

        // Load credentials (private key, address)
        Credentials credentials = Credentials.create(derivedKeyPair);

        System.out.println("Wallet Address: " + credentials.getAddress());
        return credentials;
    }


}



