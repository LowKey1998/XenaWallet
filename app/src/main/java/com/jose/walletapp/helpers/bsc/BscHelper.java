package com.jose.walletapp.helpers.bsc;

import org.json.JSONObject;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.Arrays;

public class BscHelper {

    private final String rpcUrl="https://bsc-dataseed.binance.org/";
    private Web3j web3;

    public BscHelper() {
        web3 = Web3j.build(new HttpService(rpcUrl));
    }

    // Fetch token info: name, symbol, decimals, logo
    public JSONObject getTokenInfo(String contractAddress) {
        JSONObject tokenInfo = new JSONObject();
        try {
            tokenInfo.put("name", callContractFunction(contractAddress, "name"));
            tokenInfo.put("symbol", callContractFunction(contractAddress, "symbol"));
            tokenInfo.put("decimals", Integer.parseInt(callContractFunction(contractAddress, "decimals")));
            tokenInfo.put("logo", "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/binance/assets/"
                    + contractAddress + "/logo.png");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tokenInfo;
    }

    private String callContractFunction(String contractAddress, String functionName) throws Exception {
        Function function;
        if(functionName.equals("decimals")) {
            function = new Function(functionName, Arrays.asList(), Arrays.asList(new TypeReference<Uint8>() {}));
        } else {
            function = new Function(functionName, Arrays.asList(), Arrays.asList(new TypeReference<Utf8String>() {}));
        }

        EthCall response = web3.ethCall(
                org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                        "0x0000000000000000000000000000000000000000", contractAddress, FunctionEncoder.encode(function)
                ),
                DefaultBlockParameterName.LATEST
        ).send();

        return FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters()).get(0).getValue().toString();
    }

    // Send ERC20 token
    public String sendToken(String privateKey, String contractAddress, String toAddress, BigInteger amount, BigInteger gasPrice, BigInteger gasLimit, BigInteger nonce) {
        try {
            Credentials credentials = Credentials.create(privateKey);
            Function function = new Function(
                    "transfer",
                    Arrays.asList(new Address(toAddress), new Uint256(amount)),
                    Arrays.asList(new TypeReference<org.web3j.abi.datatypes.Bool>() {})
            );
            String encodedFunction = FunctionEncoder.encode(function);

            RawTransaction rawTransaction = RawTransaction.createTransaction(
                    nonce,
                    gasPrice,
                    gasLimit,
                    contractAddress,
                    BigInteger.ZERO,
                    encodedFunction
            );

            byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
            String hexValue = Numeric.toHexString(signedMessage);
            EthSendTransaction transactionResponse = web3.ethSendRawTransaction(hexValue).send();
            return transactionResponse.getTransactionHash();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

