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


    public static String getMyAddress(Context context) {
        Credentials credentials = null;
        try {
            ECKeyPair keyPair = ECKeyPair.create(ECKeyStorage.loadPrivateKey(context));
            credentials = Credentials.create(keyPair);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String myAddress = credentials.getAddress();
        return myAddress;
    }



    public static String getMySolanaAddress(Context context) {

        return null;
    }



    public static BigDecimal getWalletBalance(String address) {
        try {
            // Connect to Infura or other node
            Web3j web3 = Web3j.build(new HttpService("https://polygon-mainnet.infura.io/v3/"+INFURA_KEY));

            // Get ETH balance in Wei
            BigInteger balanceWei = web3
                    .ethGetBalance(address, DefaultBlockParameterName.LATEST)
                    .send()
                    .getBalance();

            // Convert Wei to Ether
            BigDecimal balanceEther = Convert.fromWei(new BigDecimal(balanceWei), Convert.Unit.ETHER);

            System.out.println(" Balance: " + balanceEther + " ");
            return balanceEther;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Double getSolanaBalance(String address){
        RpcClient rpcClient=new RpcClient("https://api.mainnet-beta.solana.com");
        try{
            PublicKey publicKey=new PublicKey(address);
            Double solBalance=rpcClient.getApi().getBalance(publicKey)/1_000_000_000.0;
            return solBalance;
        } catch (Exception e) {
           // throw new RuntimeException(e);
            return null;
        }

    }
/*

    public static Double getUsdSolanaBalance(Double solBalance){
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("https://api.coingecko.com/api/v3/simple/price?ids=solana&vs_currencies=usd")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                JSONObject json = new JSONObject(body);
                double solUsdPrice = json.getJSONObject("solana").getDouble("usd");

                // Example: convert balance to USD
                double usdValue = solBalance * solUsdPrice;

                Log.d("SOL_USD", "USD Value: $" + usdValue);
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }
        });

        return;
    }
*/
    public static BigDecimal getMaticBalance(String address){
        Web3j web3 = Web3j.build(new HttpService("https://polygon-mainnet.infura.io/v3/"+INFURA_KEY));
        EthGetBalance balanceWei = null;
        try {
            balanceWei = web3.ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
            BigDecimal matic = Convert.fromWei(balanceWei.getBalance().toString(), Convert.Unit.ETHER);
            System.out.println("Balance: " + matic.toPlainString() + " MATIC");
            return matic;
        } catch (IOException e) {
            //throw new RuntimeException(e);
            return null;
        }

    }

    public static BigDecimal getUsdtBalance(String address, String privateKey){

        try {
            String USDT_CONTRACT = "0xc7198437980c041c805A1EDcbA50c1Ce5db95118"; // Verified Avalanche C-Chain
            String RPC_URL = "https://api.avax.network/ext/bc/C/rpc";


            Web3j web3j = Web3j.build(new HttpService(RPC_URL));
            Credentials credentials = Credentials.create(privateKey);

            ERC20 usdt = new ERC20(
                    USDT_CONTRACT,
                    web3j,
                    credentials,
                    new StaticGasProvider(
                            Convert.toWei("50", Convert.Unit.GWEI).toBigInteger(),
                            BigInteger.valueOf(100_000)
                    )
            );

            BigInteger balance = usdt.balanceOf(credentials.getAddress()).send();
            BigDecimal readable = new BigDecimal(balance).divide(BigDecimal.TEN.pow(6));

            return readable;
        } catch (Exception e) {
            //throw new RuntimeException(e);
            return null;
        }

    }

    public static String sendUsdt(Context context, String toAddress, float amnt) {
        try {
            String USDT_CONTRACT = "0xc7198437980c041c805A1EDcbA50c1Ce5db95118"; // Verified Avalanche C-Chain
            String RPC_URL = "https://api.avax.network/ext/bc/C/rpc";


            Web3j web3j = Web3j.build(new HttpService(RPC_URL));
            ECKeyPair keyPair = ECKeyPair.create(ECKeyStorage.loadPrivateKey(context));
            Credentials credentials = Credentials.create(keyPair);

            ERC20 usdt = new ERC20(
                    USDT_CONTRACT,
                    web3j,
                    credentials,
                    new StaticGasProvider(
                            Convert.toWei("50", Convert.Unit.GWEI).toBigInteger(),
                            BigInteger.valueOf(100_000)
                    )
            );

            BigInteger amount = new BigInteger((amnt*1000000)+""); // 1 USDT (6 decimals)
            TransactionReceipt receipt = usdt.transfer(toAddress, amount).send();
            Log.d("TX", "Sent USDT: " + receipt.getTransactionHash());
            return receipt.getTransactionHash();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public static void sendPolygon(String privateKey, String toAddress) {
        try {
            // 1. Setup
            Web3j web3 = Web3j.build(new HttpService("https://polygon-mainnet.infura.io/v3/"+INFURA_KEY));
            Credentials credentials = Credentials.create(privateKey);

            // 2. Send transaction
            var transactionReceipt = Transfer.sendFunds(
                    web3,
                    credentials,
                    toAddress,
                    BigDecimal.valueOf(0.1),  // matic
                    Convert.Unit.ETHER
            ).send();

            System.out.println("Transaction hash: " + transactionReceipt.getTransactionHash());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}



