package com.example.etherumrpc;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.web3j.abi.TypeDecoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.contracts.eip20.generated.ERC20;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.Transfer;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;

@SpringBootTest
public class RpcTests {

    private final String rpcEndPoint = "https://mainnet.infura.io/v3/27871fde3d9e4eddb88368045cecb18b";

    @Test
    void contextLoads() {
    }

    @Test
    public void getGethInfo() throws Exception {
        Web3j web3j = Web3j.build(new HttpService(rpcEndPoint));
        Web3ClientVersion web3ClientVersion = web3j.web3ClientVersion().send();
        System.out.println(web3ClientVersion.getWeb3ClientVersion());
    }

    @Test
    public void generateEthKey() throws Exception {
        ECKeyPair keyPair = Keys.createEcKeyPair();
        BigInteger publicKey = keyPair.getPublicKey();
        String publicKeyHex = Numeric.toHexStringWithPrefix(publicKey);
        System.out.println(publicKeyHex);

        BigInteger privateKey = keyPair.getPrivateKey();
        String privateKeyHex = Numeric.toHexStringWithPrefix(privateKey);
        System.out.println(privateKeyHex);

        Credentials credentials = Credentials.create(new ECKeyPair(privateKey, publicKey));
        String address = credentials.getAddress();
        System.out.println(address);
    }

    @Test
    public void getBlockNumber() throws Exception {
        Web3j web3j = Web3j.build(new HttpService(rpcEndPoint));

        EthBlockNumber ethBlockNumber = web3j.ethBlockNumber().send();
        System.out.println("eth Block Number : " + ethBlockNumber.getBlockNumber());
    }

    @Test
    public void getBlockNumberByTransaction() throws Exception {
        Web3j web3j = Web3j.build(new HttpService(rpcEndPoint));

        EthBlockNumber ethBlockNumber = web3j.ethBlockNumber().send();
        System.out.println("eth Block Number : " + ethBlockNumber.getBlockNumber());

        DefaultBlockParameter blockParameter = DefaultBlockParameter.valueOf(ethBlockNumber.getBlockNumber());
        EthBlock ethBlock = web3j.ethGetBlockByNumber(blockParameter, true).send();

        for (EthBlock.TransactionResult txResult : ethBlock.getBlock().getTransactions()) {

            EthBlock.TransactionObject txObject = (EthBlock.TransactionObject) txResult;
            System.out.println("tx hash : " + txObject.getHash());
            /**
             * ....
             */
        }
    }

    /**
     * decode transaction inputData
     * amount unit : wei
     *
     * @throws Exception
     */
    @Test
    public void decodeTransactionInputData() throws Exception {
        String inputData = "0xa9059cbb0000000000000000000000003f156f405b1213c40170e47e91d42222266c56f700000000000000000000000000000000000000000000151d9c67ba73fe9ac000";
        String method = inputData.substring(0, 10);
        System.out.println(method);

        String to = inputData.substring(10, 74);
        Method refMethod = TypeDecoder.class.getDeclaredMethod("decode", String.class, int.class, Class.class);
        refMethod.setAccessible(true);
        Address address = (Address) refMethod.invoke(null, to, 0, Address.class);
        System.out.println(address.toString());

        String value = inputData.substring(74);
        Uint256 amount = (Uint256) refMethod.invoke(null, value, 0, Uint256.class);
        System.out.println(amount.getValue());
    }

    @Test
    public void getTokenTransactionInfo() throws Exception {
        Web3j web3j = Web3j.build(new HttpService(rpcEndPoint));

        String txHash = "";
        EthTransaction ethTransaction = web3j.ethGetTransactionByHash(txHash).send();
        Optional<Transaction> txInfo = ethTransaction.getTransaction();
        String inputData = txInfo.get().getInput();

        System.out.println("input stirng : " + inputData);
        if (!inputData.substring(0, 10).equals("0xa9059cbb")) {
            System.out.println("Not a transfer function");
        } else {
            String toAddress = "0x" + inputData.substring(34, 74);
            System.out.println("to address : " + toAddress);

            String amount = inputData.substring(74, 138);
            BigInteger weiAmount = new BigInteger(amount, 16);
            System.out.println("amount : " + weiAmount);
        }
    }

    @Test
    public void getGasPrice() throws Exception {
        Web3j web3j = Web3j.build(new HttpService(rpcEndPoint));
        EthGasPrice gasPrice = web3j.ethGasPrice().send();

        System.out.println(gasPrice.getGasPrice());
    }

    @Test
    public void getAccountNonce() throws Exception {
        Web3j web3j = Web3j.build(new HttpService(rpcEndPoint));

        String address = "";
        EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                address, DefaultBlockParameterName.LATEST).send();
        BigInteger nonce = ethGetTransactionCount.getTransactionCount();

        System.out.println(nonce);
    }

    @Test
    public void transferEth() throws Exception {
        Web3j web3j = Web3j.build(new HttpService(rpcEndPoint));

        String privateKey = "";
        Credentials credentials = Credentials.create(privateKey);

        String toAddress = "";
        TransactionReceipt transactionReceipt = Transfer.sendFunds(web3j, credentials, toAddress, BigDecimal.TEN, Convert.Unit.ETHER).send();
        System.out.println("hash : " + transactionReceipt.getTransactionHash());
    }

    @Test
    public void transferToken() throws Exception {
        Web3j web3j = Web3j.build(new HttpService(rpcEndPoint));

        String privateKey = "";
        Credentials credentials = Credentials.create(privateKey);

        String contractAddress = "";
        ERC20 erc20 = ERC20.load(contractAddress, web3j, credentials, getContractGasProvider(web3j));

        BigDecimal sendAmount = BigDecimal.ONE;
        BigInteger amount = Convert.toWei(sendAmount, Convert.Unit.ETHER).toBigInteger();

        String toAddress = "";
        TransactionReceipt transactionReceipt = erc20.transfer(toAddress, amount).send();
        System.out.println(transactionReceipt.getTransactionHash());
    }

    public ContractGasProvider getContractGasProvider(Web3j web3j) {
        return new ContractGasProvider() {
            @Override
            public BigInteger getGasPrice(String s) {
                try {
                    EthGasPrice gasPrice = web3j.ethGasPrice().send();
                    return gasPrice.getGasPrice();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public BigInteger getGasLimit(String s) {
                return BigInteger.valueOf(21000L); // contract Gas Limit
            }

            @Override
            public BigInteger getGasPrice() {
                return null;
            }

            @Override
            public BigInteger getGasLimit() {
                return null;
            }
        };
    }

    @Test
    public void getBalance(String address) throws Exception {
        Web3j web3j = Web3j.build(new HttpService(rpcEndPoint));

        BigDecimal unit = BigDecimal.TEN.pow(18);

        DefaultBlockParameter blockParameter = DefaultBlockParameter.valueOf(web3j.ethBlockNumber().send().getBlockNumber());
        EthGetBalance ethGetBalance = web3j.ethGetBalance(address, blockParameter).send();
        System.out.println(new BigDecimal(ethGetBalance.getBalance()).divide(unit));
    }

    @Test
    public void getTokenBalance() throws Exception {
        Web3j web3j = Web3j.build(new HttpService(rpcEndPoint));

        String privateKey = "";
        Credentials credentials = Credentials.create(privateKey);

        BigDecimal unit = BigDecimal.TEN.pow(18);

        String contractAddress = "";
        ERC20 berryToken = ERC20.load(contractAddress, web3j, credentials, new DefaultGasProvider());
        String address = "";
        BigInteger balance = berryToken.balanceOf(address).send();
        System.out.println(new BigDecimal(balance).divide(unit));
    }

    @Test
    public TransactionReceipt getTransactionReceipt() throws IOException {
        Web3j web3j = Web3j.build(new HttpService(rpcEndPoint));

        String txHash = "";
        return web3j.ethGetTransactionReceipt(txHash).send().getTransactionReceipt().get();
    }
}
