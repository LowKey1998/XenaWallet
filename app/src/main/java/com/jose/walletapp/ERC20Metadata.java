package com.jose.walletapp;

import org.json.JSONObject;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.http.HttpService;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

//ETH/BSC
public class ERC20Metadata {

    public static String callStringFunction(Web3j web3j, String contract, String functionName) throws Exception {
        Function function = new Function(
                functionName,
                Arrays.asList(),
                Arrays.asList(new TypeReference<Utf8String>() {})
        );

        String encoded = FunctionEncoder.encode(function);
        Transaction transaction = Transaction.createEthCallTransaction(null, contract, encoded);
        EthCall response = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).send();
        List<Type> decoded = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
        return decoded.isEmpty() ? null : decoded.get(0).getValue().toString();
    }

    public static int callUint8Function(Web3j web3j, String contract, String functionName) throws Exception {
        Function function = new Function(
                functionName,
                Arrays.asList(),
                Arrays.asList(new TypeReference<Uint8>() {})
        );

        String encoded = FunctionEncoder.encode(function);
        Transaction transaction = Transaction.createEthCallTransaction(null, contract, encoded);
        EthCall response = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).send();
        List<Type> decoded = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
        return decoded.isEmpty() ? -1 : ((Uint8) decoded.get(0)).getValue().intValue();
    }

    // Fetch logo from CoinGecko
    public static String fetchLogoFromCoinGecko(String chain, String contractAddress) throws Exception {
        String apiUrl = "https://api.coingecko.com/api/v3/coins/" + chain + "/contract/" + contractAddress;
        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
        conn.setRequestMethod("GET");
        conn.connect();

        Scanner sc = new Scanner(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        while (sc.hasNext()) sb.append(sc.nextLine());
        sc.close();

        JSONObject json = new JSONObject(sb.toString());
        return json.getJSONObject("image").getString("large");
    }
}

