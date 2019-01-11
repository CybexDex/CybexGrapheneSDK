package cy.agorise.graphenej;

import com.google.common.primitives.UnsignedLong;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;

import org.bitcoinj.core.ECKey;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.net.ssl.SSLContext;

import cy.agorise.graphenej.api.GetLimitOrders;
import cy.agorise.graphenej.api.TransactionBroadcastSequence;
import cy.agorise.graphenej.errors.MalformedAddressException;
import cy.agorise.graphenej.interfaces.WitnessResponseListener;
import cy.agorise.graphenej.models.BaseResponse;
import cy.agorise.graphenej.models.WitnessResponse;
import cy.agorise.graphenej.objects.Memo;
import cy.agorise.graphenej.operations.CreateAccountOperation;
import cy.agorise.graphenej.operations.CreateAccountOperationBuilder;
import cy.agorise.graphenej.operations.CustomOperation;
import cy.agorise.graphenej.operations.LimitOrderCancelOperation;
import cy.agorise.graphenej.operations.LimitOrderCreateOperation;
import cy.agorise.graphenej.operations.TransferOperation;
import cy.agorise.graphenej.operations.TransferOperationBuilder;
import cy.agorise.graphenej.test.NaiveSSLContext;

public class TransactionTest {
    private final String BILTHON_7_BRAIN_KEY = TestAccounts.Bilthon7.BRAINKEY;
    private final String BILTHON_5_BRAIN_KEY = TestAccounts.Bilthon5.BRAINKEY;
    private final String BILTHON_16_BRAIN_KEY = TestAccounts.Bilthon16.BRAINKEY;

    private final String NODE_URL = "wss://hongkong.cybex.io/";
    private final String NODE_URL_2 = "wss://shanghai.51nebula.com/";

    // Transfer operation transaction
    private final Asset CYB = new Asset("1.3.0");
    private final UserAccount cybx_1 = new UserAccount("1.2.28828");
    private final UserAccount cybx_2 = new UserAccount("1.2.18");

    // Limit order create transaction
    private final Asset JADEETH = new Asset("1.3.2");
    private UserAccount seller = cybx_1;
    private AssetAmount amountToSell = new AssetAmount(UnsignedLong.valueOf(100000), CYB);
    private AssetAmount minToReceive = new AssetAmount(UnsignedLong.valueOf(520), JADEETH);
    private long expiration;

    // Custom operation transaction
    private final AssetAmount fee = new AssetAmount(UnsignedLong.valueOf(100000), CYB);
    private final UserAccount payer = cybx_1;
    private final Integer operationId = 61166;
    private final List<UserAccount> requiredAuths = Collections.singletonList(payer);
    private final String data = "some data";

    private final long FEE_AMOUNT = 0;

    // Lock object
    private static final class Lock {
    }

    private final Object lockObject = new Lock();

    // Response
    private BaseResponse baseResponse;

    /**
     * Generic witness response listener that will just release the lock created in
     * main thread.
     */
    WitnessResponseListener listener = new WitnessResponseListener() {

        @Override
        public void onSuccess(WitnessResponse response) {
            System.out.println("onSuccess");
            baseResponse = response;
            synchronized (this) {
                this.notifyAll();
            }
        }

        @Override
        public void onError(BaseResponse.Error error) {
            System.out.println("onError. Msg: " + error.data.message);
            synchronized (this) {
                notifyAll();
            }
        }
    };

    @Before
    public void setup() {
    }

    /**
     * Receives the elements required for building a transaction, puts them together and broadcasts it.
     *
     * @param privateKey:       The private key used to sign the transaction.
     * @param operationList:    The list of operations to include
     * @param responseListener: The response listener.
     * @param lockObject:       Optional object to use as a lock
     */
    private void broadcastTransaction(ECKey privateKey, List<BaseOperation> operationList, WitnessResponseListener responseListener, Object lockObject) {
        try {
            Transaction transaction = new Transaction(privateKey, null, operationList);

            SSLContext context = null;
            context = NaiveSSLContext.getInstance("TLS");
            WebSocketFactory factory = new WebSocketFactory();

            // Set the custom SSL context.
            factory.setSSLContext(context);

            WebSocket mWebSocket = factory.createSocket(NODE_URL_2);

            mWebSocket.addListener(new TransactionBroadcastSequence(transaction, CYB, responseListener));
            mWebSocket.connect();

            // If a lock object is specified, we use it
            if (lockObject != null) {
                synchronized (lockObject) {
                    lockObject.wait();
                }
            } else {
                // Otherwise we just use this listener as the lock
                synchronized (responseListener) {
                    responseListener.wait();
                }
            }
            Assert.assertNotNull(baseResponse);
            Assert.assertNull(baseResponse.error);
        } catch (NoSuchAlgorithmException e) {
            System.out.println("NoSuchAlgoritmException. Msg: " + e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("InterruptedException. Msg: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IOException. Msg: " + e.getMessage());
        } catch (WebSocketException e) {
            System.out.println("WebSocketException. Msg: " + e.getMessage());
        }
    }


    @Test
    public void testTransferTransaction() {
        ECKey privateKey = new AccountNamePassword("fingoes-test1activefingoesqwer1234").getPrivateKey();
        //get receive account public key
        try {
            PublicKey receivePublickey = new Address("CYB7ynbVkadWKrSkFTg2nvmNRM9sUURx1PccNYk7PRmjLxFeULdAN").getPublicKey();
            // Creating memo
            BigInteger nonce = BigInteger.ONE;
            byte[] encryptedMessage = Memo.encryptMessage(privateKey, receivePublickey, nonce, "another message");
            System.out.println(new Address(ECKey.fromPublicOnly(privateKey.getPubKey())).toString());
            Memo memo = new Memo(new Address(ECKey.fromPublicOnly(privateKey.getPubKey())), new Address(receivePublickey.getKey()), nonce, encryptedMessage);

            // Creating operation 1
            TransferOperation transferOperation1 = new TransferOperationBuilder()
                    .setTransferAmount(new AssetAmount(UnsignedLong.valueOf(1), CYB))
                    .setSource(cybx_1)
                    .setDestination(cybx_2)
                    .setFee(new AssetAmount(UnsignedLong.valueOf(FEE_AMOUNT), CYB))
                    .setMemo(memo)
                    .build();

            // Adding operations to the operation list
            ArrayList<BaseOperation> operationList = new ArrayList<>();
            operationList.add(transferOperation1);


            // Broadcasting transaction
            broadcastTransaction(privateKey, operationList, listener, null);
        } catch (MalformedAddressException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCreateAccountTransaction() {
            ECKey privateActiveKey = new AccountNamePassword("fingoes-test233activefingoesqwer1234").getPrivateKey();
            ECKey privateOwnerKey = new AccountNamePassword("fingoes-test233ownerfingoesqwer1234").getPrivateKey();
            ECKey privateRegisterActiveKey = new AccountNamePassword("jade-gatewayactiveqwer1234qwer1234").getPrivateKey();
            System.out.println(new PublicKey(ECKey.fromPublicOnly(privateRegisterActiveKey.getPubKey())).getAddress());
            PublicKey publicActiveKey = new PublicKey(ECKey.fromPublicOnly(privateActiveKey.getPubKey()));
            PublicKey publicOwnerKey = new PublicKey(ECKey.fromPublicOnly(privateOwnerKey.getPubKey()));
            PublicKey publicMemoKey = new PublicKey(ECKey.fromPublicOnly(privateOwnerKey.getPubKey()));

            HashMap<PublicKey, Long> activeKeyMap = new HashMap<>();
            HashMap<PublicKey, Long> ownerKeyMap = new HashMap<>();
            activeKeyMap.put(publicActiveKey, 1l);
            ownerKeyMap.put(publicOwnerKey, 1l);
            Authority activeAuthority = new Authority(1, activeKeyMap, null);
            Authority ownerAuthority = new Authority(1, ownerKeyMap, null);
            AccountOptions options = new AccountOptions();
            options.setMemoKey(publicMemoKey);
            options.setNumWitness(0);
            options.setNum_comittee(0);
            options.setVotingAccount(new UserAccount("1.2.5"));

            String name = "fingoes-test234";

            CreateAccountOperation createAccountOperation = new CreateAccountOperationBuilder()
                    .setActive(activeAuthority)
                    .setName(name)
                    .setOwner(ownerAuthority)
                    .setReferrer(new UserAccount("1.2.38"))
                    .setReferrer_percent(0)
                    .setRegistrar(new UserAccount("1.2.38"))
                    .setOptions(options)
                    .setFee(new AssetAmount(UnsignedLong.valueOf(FEE_AMOUNT), CYB))
                    .build();
            byte[] serilize = createAccountOperation.toBytes();
            System.out.println("Fee: " + Util.bytesToHex(new AssetAmount(UnsignedLong.valueOf(515), CYB).toBytes()));
            System.out.println("Registrar: " + Util.bytesToHex(new UserAccount("1.2.38").toBytes()));
            System.out.println("Referrer: " + Util.bytesToHex(new UserAccount("1.2.38").toBytes()));
            System.out.println("Referrer_percent: " + Util.bytesToHex(Util.revertShort((short)0)));
            System.out.println("NameLength: " + Util.bytesToHex(Util.serializeLongToBytes(name.length())));
            System.out.println("Name: " + Util.bytesToHex(Util.hexlify(name)));
            System.out.println("Owner: " + Util.bytesToHex(ownerAuthority.toBytes()));
            System.out.println("Active: " + Util.bytesToHex(activeAuthority.toBytes()));
            System.out.println("Options: " + Util.bytesToHex(options.toBytes()));
            System.out.println("serialized: "+ Util.bytesToHex(serilize));
            ArrayList<BaseOperation> operationList = new ArrayList<>();
            operationList.add(createAccountOperation);
            broadcastTransaction(privateRegisterActiveKey, operationList, listener, null);

    }

    @Test
    public void testLimitOrderCreateTransaction() {
        ECKey privateKey = new BrainKey(BILTHON_7_BRAIN_KEY, 0).getPrivateKey();
        expiration = (System.currentTimeMillis() / 1000) + 60 * 60;

        // Creating limit order creation operation
        LimitOrderCreateOperation operation = new LimitOrderCreateOperation(seller, amountToSell, minToReceive, (int) expiration, false);
        operation.setFee(new AssetAmount(UnsignedLong.valueOf(2), CYB));

        ArrayList<BaseOperation> operationList = new ArrayList<>();
        operationList.add(operation);

        // Broadcasting transaction
        broadcastTransaction(privateKey, operationList, listener, null);
    }

    /**
     * Since tests should be independent of each other, in order to be able to test the cancellation of an
     * existing order we must first proceed to create one. And after creating one, we must also retrieve
     * its id in a separate call.
     * <p>
     * All of this just makes this test a bit more complex, since we have 3 clearly defined tasks that require
     * network communication
     * <p>
     * 1- Create order
     * 2- Retrieve order id
     * 3- Send order cancellation tx
     * <p>
     * Only the last one is what we actually want to test
     *
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws WebSocketException
     */
    @Test
    public void testLimitOrderCancelTransaction() throws NoSuchAlgorithmException, IOException, WebSocketException {

        // We first must create a limit order for this test
        ECKey privateKey = new BrainKey(BILTHON_7_BRAIN_KEY, 0).getPrivateKey();
        expiration = (System.currentTimeMillis() / 1000) + 60 * 5;

        // Creating limit order creation operation
        LimitOrderCreateOperation operation = new LimitOrderCreateOperation(seller, amountToSell, minToReceive, (int) expiration, false);
        operation.setFee(new AssetAmount(UnsignedLong.valueOf(2), CYB));

        ArrayList<BaseOperation> operationList = new ArrayList<>();
        operationList.add(operation);

        // Broadcasting transaction (Task 1)
        broadcastTransaction(privateKey, operationList, new WitnessResponseListener() {

            @Override
            public void onSuccess(WitnessResponse response) {

                System.out.println("onSuccess.0");
                try {
                    // Setting up the assets
                    Asset base = amountToSell.getAsset();
                    Asset quote = minToReceive.getAsset();

                    SSLContext context = NaiveSSLContext.getInstance("TLS");
                    WebSocketFactory factory = new WebSocketFactory();

                    // Set the custom SSL context.
                    factory.setSSLContext(context);
                    WebSocket mWebSocket = factory.createSocket(NODE_URL);

                    // Requesting limit order to cancel (Task 2)
                    mWebSocket.addListener(new GetLimitOrders(base.getObjectId(), quote.getObjectId(), 100, new WitnessResponseListener() {

                        @Override
                        public void onSuccess(WitnessResponse response) {
                            System.out.println("onSuccess.1");
                            List<LimitOrder> orders = (List<LimitOrder>) response.result;
                            for (LimitOrder order : orders) {
                                if (order.getSeller().getObjectId().equals(cybx_1.getObjectId())) {

                                    // Instantiating a private key for bilthon-15
                                    ECKey privateKey = new BrainKey(BILTHON_7_BRAIN_KEY, 0).getPrivateKey();

                                    // Creating limit order cancellation operation
                                    LimitOrderCancelOperation operation = new LimitOrderCancelOperation(order, cybx_1);
                                    ArrayList<BaseOperation> operationList = new ArrayList<>();
                                    operationList.add(operation);

                                    // Broadcasting order cancellation tx (Task 3)
                                    broadcastTransaction(privateKey, operationList, new WitnessResponseListener() {

                                        @Override
                                        public void onSuccess(WitnessResponse response) {
                                            System.out.println("onSuccess.2");
                                            baseResponse = response;
                                            synchronized (this) {
                                                notifyAll();
                                            }
                                            synchronized (lockObject) {
                                                lockObject.notifyAll();
                                            }
                                        }

                                        @Override
                                        public void onError(BaseResponse.Error error) {
                                            System.out.println("onError.2");
                                            synchronized (this) {
                                                notifyAll();
                                            }
                                            synchronized (lockObject) {
                                                lockObject.notifyAll();
                                            }
                                        }
                                    }, null);
                                }
                            }
                        }

                        @Override
                        public void onError(BaseResponse.Error error) {
                            System.out.println("onError.1");
                            System.out.println(error.data.message);
                            Assert.assertNull(error);
                            synchronized (lockObject) {
                                lockObject.notifyAll();
                            }
                        }
                    }));

                    mWebSocket.connect();

                } catch (NoSuchAlgorithmException e) {
                    System.out.println("NoSuchAlgorithmException. Msg: " + e.getMessage());
                } catch (WebSocketException e) {
                    System.out.println("WebSocketException. Msg: " + e.getMessage());
                } catch (IOException e) {
                    System.out.println("IOException. Msg: " + e.getMessage());
                }
            }

            @Override
            public void onError(BaseResponse.Error error) {
                System.out.println("OnError. Msg: " + error.message);
                synchronized (this) {
                    notifyAll();
                }
            }
        }, lockObject);
    }

    @Test
    public void testCustomOperationTransaction() {
        ECKey sourcePrivateKey = new BrainKey(BILTHON_7_BRAIN_KEY, 0).getPrivateKey();

        // Creating custom operation
        CustomOperation customOperation = new CustomOperation(fee, payer, operationId, requiredAuths, data);

        // Adding operation to the operation list
        ArrayList<BaseOperation> operationList = new ArrayList<>();
        operationList.add(customOperation);

        // Broadcasting transaction
        broadcastTransaction(sourcePrivateKey, operationList, listener, null);
    }
}