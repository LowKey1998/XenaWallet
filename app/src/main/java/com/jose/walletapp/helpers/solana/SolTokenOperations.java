package com.jose.walletapp.helpers.solana;

import org.p2p.solanaj.core.*;
import org.p2p.solanaj.programs.TokenProgram;
import org.p2p.solanaj.rpc.RpcClient;
import org.p2p.solanaj.rpc.Cluster;
import org.p2p.solanaj.rpc.RpcException;
import org.p2p.solanaj.rpc.types.TokenAccountInfo;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SolTokenOperations {
    private static final String RPC_URL = "https://api.mainnet-beta.solana.com";

    public static Double getSolanaNativeBalance(String address){
        RpcClient rpcClient=new RpcClient(RPC_URL);
        try{
            PublicKey publicKey=new PublicKey(address);
            Double solBalance=rpcClient.getApi().getBalance(publicKey)/1_000_000_000.0;
            return solBalance;
        }
        catch (Exception e) {
            // throw new RuntimeException(e);
         return null;
        }
    }


    /**
     * Get SPL token balance for a user
     *
     * @param walletAddress  User's wallet address
     * @param tokenMintAddress Token contract (mint) address
     * @return Total token balance (double), 0.0 if none
     */
    public static double getUserSplTokenBalance(String walletAddress, String tokenMintAddress) {
        try {
            RpcClient client = new RpcClient(RPC_URL);
            PublicKey owner = new PublicKey(walletAddress);

            // Filter by mint
            Map<String, Object> filter = new HashMap<>();
            filter.put("mint", tokenMintAddress);

            // Use JSON parsed encoding to get uiAmount
            Map<String, Object> config = new HashMap<>();
            config.put("encoding", "jsonParsed");

            // Fetch token accounts
            TokenAccountInfo info = client.getApi().getTokenAccountsByOwner(owner, filter, config);

            double totalBalance = 0.0;

            if (info != null && info.getValue() != null) {
                List<TokenAccountInfo.Value> accounts = info.getValue();
                for (TokenAccountInfo.Value acc : accounts) {
                    if (acc.getAccount() != null &&
                            acc.getAccount().getData() != null &&
                            acc.getAccount().getData().getParsed() != null &&
                            acc.getAccount().getData().getParsed().getInfo() != null &&
                            acc.getAccount().getData().getParsed().getInfo().getTokenAmount() != null) {

                        totalBalance += acc.getAccount().getData().getParsed().getInfo().getTokenAmount().getUiAmount();
                    }
                }
            }

            return totalBalance;

        } catch (Exception e) {
            e.printStackTrace();
            return 0.0;
        }
    }
}
