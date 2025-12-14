package com.jose.walletapp.helpers.crosschain;


import org.bitcoinj.core.Base58;
import org.p2p.solanaj.core.Account;
import org.p2p.solanaj.core.PublicKey;
import org.p2p.solanaj.programs.TokenProgram;
import org.p2p.solanaj.rpc.RpcClient;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.Transfer;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.crypto.*;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CryptoTransferManager {

    private static final String BSC_FEE_CONTRACT = "0xYourBscFeeContract";
    private static final String TRON_FEE_CONTRACT = "TYourTronFeeContract";
    private static final BigInteger FEE_BSC = new BigInteger("100000");    // 0.1 USDT
    private static final BigInteger FEE_TRON = new BigInteger("500000");   // 0.5 USDT

    //private RangoService rangoService = new RangoService();

/*
    public String sendWithAutoBridge(
        Chain sourceChain,
        String privateKey,
        String toAddress,
        String tokenAddress,
        BigInteger amount,
        String rpcOrNodeUrl,
        Chain targetChain,
        String targetTokenAddress,
        List<String> allowedBridgeChains
    ) throws Exception {
        boolean hasEnough = checkBalance(sourceChain, privateKey, tokenAddress, amount, rpcOrNodeUrl);

        if (hasEnough) {
            return send(sourceChain, privateKey, toAddress, tokenAddress, amount, rpcOrNodeUrl);
        }

        String fromAddress = getAddressFromPrivateKey(sourceChain, privateKey);
        JSONObject quote = rangoService.getBestRoute(
            sourceChain.name().toLowerCase(),
            tokenAddress,
            targetChain.name().toLowerCase(),
            targetTokenAddress,
            amount.toString(),
            fromAddress,
            toAddress,
            allowedBridgeChains
        );

        JSONObject txRequest = quote.getJSONObject("transactionRequest");

        switch (sourceChain) {
            case BSC:
                return forwardViaBscFeeContract(privateKey, tokenAddress, amount, txRequest, rpcOrNodeUrl);
            case TRON:
                return forwardViaTronFeeContract(privateKey, tokenAddress, amount, txRequest, rpcOrNodeUrl);
            case SOLANA:
                //return sendSolana(privateKey, toAddress, tokenAddress, amount, rpcOrNodeUrl); // handle fee off-chain
            default:
                throw new Exception("Unsupported source chain for fee routing");
        }
    }
*/

    public String send(
        Chain chain,
        String privateKey,
        String toAddress,
        String tokenAddress,
        BigInteger amount,
        String rpcUrl
    ) throws Exception {
        switch (chain) {
            case BSC:
                return sendBsc(privateKey, toAddress, tokenAddress, amount, rpcUrl);
            case TRON:
                //return sendTron(privateKey, toAddress, tokenAddress, amount, rpcUrl);
            case SOLANA:
               // return sendSolana(privateKey, toAddress, tokenAddress, amount, rpcUrl);
            default:
                throw new Exception("Unsupported chain");
        }
    }

    private String sendBsc(String privateKey, String to, String token, BigInteger amount, String rpcUrl) throws Exception {
        Web3j web3 = Web3j.build(new HttpService(rpcUrl));
        Credentials credentials = Credentials.create(privateKey);

        Function function = new Function(
            "transfer",
            Arrays.asList(new Address(to), new Uint256(amount)),
            Collections.emptyList()
        );
        String encodedFunction = FunctionEncoder.encode(function);

        BigInteger nonce = web3.ethGetTransactionCount(credentials.getAddress(), DefaultBlockParameterName.LATEST)
                               .send().getTransactionCount();
        BigInteger gasPrice = web3.ethGasPrice().send().getGasPrice();
        BigInteger gasLimit = BigInteger.valueOf(60000);

        RawTransaction rawTransaction = RawTransaction.createTransaction(
            nonce, gasPrice, gasLimit, token, BigInteger.ZERO, encodedFunction
        );

        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        String hexValue = Numeric.toHexString(signedMessage);
        return web3.ethSendRawTransaction(hexValue).send().getTransactionHash();
    }

    

    private String forwardViaBscFeeContract(String privateKey, String token, BigInteger totalAmount, JSONObject txRequest, String rpcUrl) throws Exception {
        Web3j web3 = Web3j.build(new HttpService(rpcUrl));
        Credentials credentials = Credentials.create(privateKey);

        String rangoRouter = txRequest.getString("to");
        String calldata = txRequest.getString("data");

        Function function = new Function(
            "forwardWithFee",
            Arrays.asList(
                new Address(token),
                new Uint256(totalAmount),
                new Address(rangoRouter),
                new DynamicBytes(Numeric.hexStringToByteArray(calldata))
            ),
            Collections.emptyList()
        );

        String encoded = FunctionEncoder.encode(function);
        BigInteger gasPrice = web3.ethGasPrice().send().getGasPrice();
        BigInteger gasLimit = BigInteger.valueOf(200000);
        BigInteger nonce = web3.ethGetTransactionCount(credentials.getAddress(), DefaultBlockParameterName.LATEST)
                               .send().getTransactionCount();

        RawTransaction rawTx = RawTransaction.createTransaction(
            nonce, gasPrice, gasLimit, BSC_FEE_CONTRACT, BigInteger.ZERO, encoded
        );

        byte[] signedMessage = TransactionEncoder.signMessage(rawTx, credentials);
        String hexValue = Numeric.toHexString(signedMessage);
        return web3.ethSendRawTransaction(hexValue).send().getTransactionHash();
    }

    private String forwardViaTronFeeContract(String privateKey, String token, BigInteger totalAmount, JSONObject txRequest, String node) {
        // TRON: build and call sendWithFee() on deployed contract
        return "tron_contract_call_hash";
    }

    private boolean checkBalance(Chain chain, String privateKey, String token, BigInteger amount, String rpcUrl) {
        return true; // Replace with real balance check
    }

    private String getAddressFromPrivateKey(Chain chain, String privateKey) {
        if (chain == Chain.BSC) {
            return Credentials.create(privateKey).getAddress();
        }
        return "address_from_key";
    }

    enum Chain {
        BSC, TRON, SOLANA
    }
}

  /*  private String sendTron(String privateKey, String to, String token, BigInteger amount, String rpcUrl) throws Exception {
        ECKey ecKey = ECKey.fromPrivate(Hex.decode(privateKey));
        byte[] ownerAddress = ecKey.getAddress();
        byte[] contractAddress = WalletApi.decodeFromBase58Check(token);
        byte[] recipientAddress = WalletApi.decodeFromBase58Check(to);

        byte[] input = AbiUtil.encodeTransfer(recipientAddress, amount.longValue()); // assuming 6 decimals
        Contract.TriggerSmartContract trigger = Contract.TriggerSmartContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ownerAddress))
            .setContractAddress(ByteString.copyFrom(contractAddress))
            .setData(ByteString.copyFrom(input))
            .build();

        TransactionExtention txnExt = WalletApi.triggerContract(trigger);
        if (!txnExt.getResult().getResult()) {
            throw new RuntimeException("TRON contract trigger failed: " + txnExt.getResult().getMessage().toStringUtf8());
        }

        Transaction signedTxn = WalletApi.signTransaction(txnExt.getTransaction(), ecKey);
        TransactionReturn response = WalletApi.broadcastTransaction(signedTxn);
        if (!response.getResult()) {
            throw new RuntimeException("TRON broadcast failed: " + response.getMessage().toStringUtf8());
        }

        return ByteArray.toHexString(signedTxn.getTxid().toByteArray());
        return null;
    }

    private String sendSolana(String privateKey, String to, String token, BigInteger amount, String rpcUrl) throws Exception {
        // Using SolanaJ or HTTP client to build and sign transaction
        PublicKey sender = new Account(Base58.decode(privateKey)).getPublicKey();
        PublicKey recipient = new PublicKey(to);
        PublicKey tokenMint = new PublicKey(token);

        RpcClient client = new RpcClient(rpcUrl);
        TokenProgram tokenProgram = new TokenProgram();

        Transaction transaction = tokenProgram.transfer(
            tokenMint, sender, recipient, sender, amount.longValue(), null
        );

        String txHash = client.getApi().sendTransaction(transaction.serialize(), true);
        return txHash;
    }
*/