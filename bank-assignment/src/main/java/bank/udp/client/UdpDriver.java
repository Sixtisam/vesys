package bank.udp.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.PortUnreachableException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Set;

import bank.InactiveException;
import bank.OverdrawException;
import bank.udp.shared.MessageType;
import bank.udp.shared.RequestResponseDescriptor;
import bank.udp.shared.RequestResponseDescriptor.CloseAccountDescriptor;
import bank.udp.shared.RequestResponseDescriptor.CreateAccountDescriptor;
import bank.udp.shared.RequestResponseDescriptor.DepositDescriptor;
import bank.udp.shared.RequestResponseDescriptor.GetAccountActiveDescriptor;
import bank.udp.shared.RequestResponseDescriptor.GetAccountBalanceDescriptor;
import bank.udp.shared.RequestResponseDescriptor.GetAccountNumbersDescriptor;
import bank.udp.shared.RequestResponseDescriptor.GetAccountOwnerDescriptor;
import bank.udp.shared.RequestResponseDescriptor.TransferDescriptor;
import bank.udp.shared.RequestResponseDescriptor.WithdrawDescriptor;
import bank.udp.shared.RichDataInputStream;
import bank.udp.shared.RichDataOutputStream;

public class UdpDriver implements bank.BankDriver {
    public static final int ANSWER_TIMEOUT = 2000; // 2 secs
    public static final int MAX_RETRY_COUNT = 15; // 2 secs
    private Bank bank = null;
    private DatagramSocket socket;
    private InetAddress address;
    private int port;
    private int clientNr;
    private int requestNr = 0;
    private DatagramPacket receivePacket;

    @Override
    public void connect(String[] args) throws UnknownHostException, IOException {
        bank = new Bank();
        System.out.println("connected...");
        address = InetAddress.getByName(args[0]);
        port = Integer.parseInt(args[1]);
        
        socket = new DatagramSocket();
        socket.setSoTimeout(ANSWER_TIMEOUT);
        socket.connect(address, port); // constrain sending an receiving to the specified address+port
        // create a receive packet which will server as receive data buffer
        receivePacket = createPacket(new byte[65535]);
        handleHelloProtocol();
    }

    @Override
    public void disconnect() throws IOException {
        bank = null;
        socket.close();
        if (!socket.isClosed()) {
            throw new RuntimeException("Not closed");
        }
        System.out.println("disconnected...");
    }

    @Override
    public bank.Bank getBank() {
        return bank;
    }

    /**
     * Executes the hello protocol with the server.
     * This includes
     * - sending an empty packet
     * - receiving a packet which contains the assigned client id.
     * @throws IOException
     */
    public void handleHelloProtocol() throws IOException {
        DatagramPacket packet = createPacket(new byte[0]);
        sendAndReceivePacket(packet);
        DataInputStream in = getInputStream();
        clientNr = in.readInt();
    }

    /**
     * Builds an {@link RichDataInputStream} for the current receivePacket
     */
    public RichDataInputStream getInputStream() {
        return new RichDataInputStream(new ByteArrayInputStream(receivePacket.getData(), receivePacket.getOffset(),
                receivePacket.getLength()));
    }

    /**
     * Sends an udp packet and waits until a udp packet arrives.
     * This method will wait until a packet arrives.
     * If this not happens before the {@link SocketTimeoutException} occurs, the packet will be resent.
     * this will happen up to MAX_RETRY_COUNT times
     * @param toSend
     * @throws IOException when the MAX_RETRY_COUNT is reached.
     */
    public void sendAndReceivePacket(DatagramPacket toSend) throws IOException {
        int retryCount = 0;
        
        while (true) {
            socket.send(toSend);
            try {
                socket.receive(receivePacket);
                return;
            } catch (SocketTimeoutException | PortUnreachableException ex) {
                retryCount++;
                if (retryCount >= MAX_RETRY_COUNT) {
                    throw new IOException("Max retry count reached, server probably offline");
                }
                System.out.println("Retry " + retryCount + "...");
            }
        }
    }

    public DatagramPacket createPacket(byte[] data) {
        return new DatagramPacket(data, data.length, address, port);
    }

    public <T> T remoteCall(RequestResponseDescriptor<T> reqRespDesc) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        RichDataOutputStream out = new RichDataOutputStream(baos);
        // write metadata of packet
        out.writeInt(clientNr);
        out.writeInt(++requestNr);
        out.writeInt(reqRespDesc.messageType().ordinal());
        // call the request builder
        reqRespDesc.buildRequest(out);

        DatagramPacket packet = createPacket(baos.toByteArray());
        System.out.println("Sending request " + requestNr + "of type " + reqRespDesc.messageType().toString());
        sendAndReceivePacket(packet);
        System.out.println("Got response for " + requestNr);

        return processResponse(reqRespDesc);
    }

    /**
     * Processes the current receivePacket
     */
    private <T> T processResponse(RequestResponseDescriptor<T> reqRespDesc) throws IOException, Exception {
        RichDataInputStream in = getInputStream();
        int answerRequestNr = in.readInt();
        if (answerRequestNr != requestNr) {
            throw new RuntimeException("got a different message nr back than sent!");
        }
        MessageType mt = parseMessageType(in.readInt());
        // scan message type
        if (mt == MessageType.NULL) {
            return null;
        } else if (mt == MessageType.EXCEPTION) {
            // in case server reported an exception, throw that exception
            throw in.readException();
        } else {
            // else let the request builder process the response
            return reqRespDesc.processResponse(in);
        }
    }

    public MessageType parseMessageType(int ordinal) {
        MessageType[] vals = MessageType.values();
        if (ordinal < 0 || ordinal >= MessageType.values().length) {
            throw new IllegalArgumentException("expect value in range of enum but got " + ordinal);
        }
        return vals[ordinal];
    }

    public class Bank implements bank.Bank {

        @Override
        public bank.Account getAccount(String number) throws IOException {
            try {
                String owner = remoteCall(new GetAccountOwnerDescriptor(number));
                if (owner == null) {
                    return null;
                } else {
                    return new RemoteAccount(number, owner);
                }
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

        @Override
        public Set<String> getAccountNumbers() throws IOException {
            try {
                return remoteCall(new GetAccountNumbersDescriptor());
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

        @Override
        public String createAccount(String owner) throws IOException {
            try {
                return remoteCall(new CreateAccountDescriptor(owner));
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

        @Override
        public boolean closeAccount(String number) throws IOException {
            try {
                return remoteCall(new CloseAccountDescriptor(number));
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

        @Override
        public void transfer(bank.Account from, bank.Account to, double amount)
                throws IOException, OverdrawException, InactiveException {
            try {
                remoteCall(new TransferDescriptor(from.getNumber(), to.getNumber(), amount));
            } catch (InactiveException | OverdrawException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

    }

    private class RemoteAccount implements bank.Account {
        private String number;
        private String owner;

        private RemoteAccount(String number, String owner) {
            this.number = number;
            this.owner = owner;
        }

        @Override
        public String getNumber() {
            return number;
        }

        @Override
        public double getBalance() throws IOException {
            try {
                return remoteCall(new GetAccountBalanceDescriptor(this.number));
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

        @Override
        public String getOwner() throws IOException {
            return owner;
        }

        @Override
        public boolean isActive() throws IOException {
            try {
                return remoteCall(new GetAccountActiveDescriptor(this.number));
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

        @Override
        public void deposit(double amount) throws InactiveException, IllegalArgumentException, IOException {
            try {
                remoteCall(new DepositDescriptor(this.number, amount));
            } catch (InactiveException | IllegalArgumentException ex) {
                throw ex;
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

        @Override
        public void withdraw(double amount)
                throws InactiveException, IllegalArgumentException, OverdrawException, IOException {
            try {
                remoteCall(new WithdrawDescriptor(this.number, amount));
            } catch (InactiveException | IllegalArgumentException | OverdrawException ex) {
                throw ex;
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

    }
}