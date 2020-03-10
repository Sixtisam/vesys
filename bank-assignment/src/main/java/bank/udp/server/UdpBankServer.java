package bank.udp.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import bank.udp.server.RequestHandler.CloseAccountRequestHandler;
import bank.udp.server.RequestHandler.CreateAccountRequestHandler;
import bank.udp.server.RequestHandler.DepositRequestHandler;
import bank.udp.server.RequestHandler.GetAccountActiveRequestHandler;
import bank.udp.server.RequestHandler.GetAccountBalanceRequestHandler;
import bank.udp.server.RequestHandler.GetAccountNumbersRequestHandler;
import bank.udp.server.RequestHandler.GetAccountOwnerRequestHandler;
import bank.udp.server.RequestHandler.ResultIsNullException;
import bank.udp.server.RequestHandler.TransferRequestHandler;
import bank.udp.server.RequestHandler.WithdrawRequestHandler;
import bank.udp.shared.MessageType;
import bank.udp.shared.RichDataInputStream;
import bank.udp.shared.RichDataOutputStream;

public class UdpBankServer {
    public static final Map<Integer, Map<Integer, byte[]>> REQUEST_RESPONSE_LOG = Collections
            .synchronizedMap(new HashMap<>());

    public static final Map<MessageType, RequestHandler> HANDLER_MAP = Collections.synchronizedMap(new HashMap<>());

    public static final AtomicInteger CLIENT_NR_SEQUENCE = new AtomicInteger(0);

    public static ServerBank BANK;

    public static void main(String[] args) {
        System.out.println("Initializing bank server...");
        ExecutorService svc = Executors.newCachedThreadPool();
        BANK = new ServerBank();
        initializeRequestHandlerMap();
        try (DatagramSocket socket = new DatagramSocket(1234)) {
            while (true) {
                System.out.println("Waiting for udp packet...");
                DatagramPacket packet = new DatagramPacket(new byte[65535], 65535);
                socket.receive(packet);
                svc.execute(new PacketHandler(socket, packet));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void initializeRequestHandlerMap() {
        HANDLER_MAP.put(MessageType.GET_ACCOUNT_NUMBERS, new GetAccountNumbersRequestHandler());
        HANDLER_MAP.put(MessageType.CREATE_ACCOUNT, new CreateAccountRequestHandler());
        HANDLER_MAP.put(MessageType.GET_OWNER, new GetAccountOwnerRequestHandler());
        HANDLER_MAP.put(MessageType.GET_BALANCE, new GetAccountBalanceRequestHandler());
        HANDLER_MAP.put(MessageType.GET_ACTIVE, new GetAccountActiveRequestHandler());
        HANDLER_MAP.put(MessageType.DEPOSIT, new DepositRequestHandler());
        HANDLER_MAP.put(MessageType.WITHDRAW, new WithdrawRequestHandler());
        HANDLER_MAP.put(MessageType.CLOSE_ACCOUNT, new CloseAccountRequestHandler());
        HANDLER_MAP.put(MessageType.TRANSFER, new TransferRequestHandler());
    }

    /**
     * Responsible for handling one single {@link DatagramPacket} from any user.
     * 
     * @author skeeks
     *
     */
    public static class PacketHandler implements Runnable {
        private DatagramSocket socket;
        private DatagramPacket packet;

        public PacketHandler(DatagramSocket socket, DatagramPacket packet) {
            this.socket = socket;
            this.packet = packet;
        }

        @Override
        public void run() {
            try (RichDataInputStream in = new RichDataInputStream(new ByteArrayInputStream(packet.getData()))) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                RichDataOutputStream out = new RichDataOutputStream(baos);

                byte[] data;
                
                // randomly destroy request to test correct packet loss handling
                if (Math.random() > 0.95) {
                    System.out.println("Destroy request");
                    return;
                }
                
                // in case of hello packet
                if (packet.getLength() == 0) {
                    int clientNr = CLIENT_NR_SEQUENCE.incrementAndGet();
                    System.out.println("Hello Paket detected, sending clientNr=" + clientNr + " back");
                    out.writeInt(clientNr);
                    data = baos.toByteArray();
                } else {
                    // randomly destroy response to test correct packet loss handling
                    data = handleRequest(in, baos, out);
                }

                // randomly destroy response to test correct packet loss handling
                if (Math.random() > 0.95) {
                    System.out.println("Destroy response");
                    return;
                }

                // send packet to client
                packet.setData(data, 0, data.length);
                socket.send(packet);
            } catch (Exception e) {
                System.err.println("Unexpected exception in RequestHandler");
                e.printStackTrace();
            }
        }

        private byte[] handleRequest(RichDataInputStream in, ByteArrayOutputStream baos, RichDataOutputStream out)
                throws IOException {
            int clientNr = in.readInt();
            int requestNr = in.readInt();

            // resend packet if client has already submitted the same requested (IDEMPOTENZ)
            byte[] data = REQUEST_RESPONSE_LOG
                    .getOrDefault(Integer.valueOf(clientNr), Collections.emptyMap())
                    .get(requestNr);

            if (data != null) {
                System.out.println("Resend request " + requestNr + " for client " + clientNr);
                return data;
            }

            try {
                out.writeInt(requestNr);
                out.writeInt(MessageType.RESPONSE.ordinal());
                handleRequest(in, out);
            } catch (ResultIsNullException ex) {
                // this exception marks a NULL response
                baos.reset(); // discard anything written before
                out.writeInt(requestNr);
                out.writeInt(MessageType.NULL.ordinal());
            } catch (Exception e) {
                // when handler throwed, reset data and write the exception to the stream.
                baos.reset(); // discard anything written before
                out.writeInt(requestNr);
                out.writeInt(MessageType.EXCEPTION.ordinal());
                out.writeException(e);
            }

            data = baos.toByteArray();
            // safe sent data
            REQUEST_RESPONSE_LOG.computeIfAbsent(Integer.valueOf(clientNr), k -> new HashMap<>()).put(requestNr, data);
            return data;
        }

        protected void handleRequest(RichDataInputStream in, RichDataOutputStream out) throws Exception {
            int messageType = in.readInt();
            MessageType type = MessageType.values()[messageType];
            System.out.println("Handle " + type.toString() + " request");
            // get handler for that message type
            RequestHandler requestHandler = HANDLER_MAP.get(type);
            requestHandler.handle(BANK, in, out);
        }
    }
}
