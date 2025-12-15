package com.jose.walletapp.helpers.bsc;

import org.json.JSONObject;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.request.Transaction;
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
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BscHelper {

    private final String rpcUrl="https://bsc-dataseed.binance.org/";
    private Web3j web3;
    private OkHttpClient httpClient;

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


    /* ===================== NATIVE BNB BALANCE ===================== */

    public BigDecimal getNativeBalance(String walletAddress) {
        try {
            BigInteger wei = web3.ethGetBalance(
                    walletAddress,
                    DefaultBlockParameterName.LATEST
            ).send().getBalance();

            return Convert.fromWei(new BigDecimal(wei), Convert.Unit.ETHER);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }


    public BigDecimal getTokenBalance(
            String walletAddress,
            String contractAddress,
            int decimals
    ) {
        try {
            Function function = new Function(
                    "balanceOf",
                    List.of(new Address(walletAddress)),
                    List.of(new TypeReference<Uint256>() {})
            );

            String encoded = FunctionEncoder.encode(function);
            EthCall response = web3.ethCall(
                    Transaction.createEthCallTransaction(
                            walletAddress,
                            contractAddress,
                            encoded
                    ),
                    DefaultBlockParameterName.LATEST
            ).send();

            BigInteger raw = (BigInteger)
                    FunctionReturnDecoder.decode(
                            response.getValue(),
                            function.getOutputParameters()
                    ).get(0).getValue();

            return new BigDecimal(raw)
                    .divide(BigDecimal.TEN.pow(decimals), decimals, RoundingMode.DOWN);

        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Converts token amount to ANY currency (USD, ZMW, EUR, GBP, etc.)
     */
    public BigDecimal convertToCurrency(
            String coingeckoId,
            BigDecimal amount,
            String currency
    ) {
        try {
            String url =
                    "https://api.coingecko.com/api/v3/simple/price" +
                            "?ids=" + coingeckoId +
                            "&vs_currencies=" + currency.toLowerCase();

            httpClient = new OkHttpClient();
            Request request = new Request.Builder().url(url).build();
            Response response = httpClient.newCall(request).execute();

            if (!response.isSuccessful()) return BigDecimal.ZERO;

            JSONObject json = new JSONObject(response.body().string());
            BigDecimal price =
                    BigDecimal.valueOf(
                            json.getJSONObject(coingeckoId)
                                    .getDouble(currency.toLowerCase())
                    );

            return amount.multiply(price);

        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }//convertToCurrency


}


