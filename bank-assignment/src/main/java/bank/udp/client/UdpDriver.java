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
import bank.udp.shared.RequestResponseDescriptor.CreateAccountDescriptor;
import bank.udp.shared.RequestResponseDescriptor.GetAccountBalanceDescriptor;
import bank.udp.shared.RequestResponseDescriptor.GetAccountNumbersDescriptor;
import bank.udp.shared.RequestResponseDescriptor.GetAccountOwnerDescriptor;
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
	private int messageNr = 0;
	private DatagramPacket receivePacket;

	@Override
	public void connect(String[] args) throws UnknownHostException, IOException {
		bank = new Bank();
		System.out.println("connected...");
		address = InetAddress.getByName(args[0]);
		port = Integer.parseInt(args[1]);
		socket = new DatagramSocket();
		socket.setSoTimeout(ANSWER_TIMEOUT);
		socket.connect(address, port);
		receivePacket = createPacket(new byte[65535]);
		execHandshake();
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

	public void execHandshake() throws IOException {
		DatagramPacket packet = createPacket(new byte[0]);
		sendAndReceivePacket(packet);
		try (DataInputStream in = getInputStream()) {
			clientNr = in.readInt();
		}
	}

	public RichDataInputStream getInputStream() {
		return new RichDataInputStream(new ByteArrayInputStream(receivePacket.getData(), receivePacket.getOffset(),
				receivePacket.getLength()));
	}

	public void sendAndReceivePacket(DatagramPacket toSend) throws IOException {
		int retryCount = 0;
		while (true) {
			socket.send(toSend);
			System.out.println("Sent packet");
			try {
				socket.receive(receivePacket);
				System.out.println("Just received packet");
				return;
			} catch (SocketTimeoutException | PortUnreachableException ex) {
				retryCount++;
				if (retryCount >= MAX_RETRY_COUNT) {
					throw new IOException("Max retry count reached, server probably offline");
				}
				System.out.println("Retry...");
			}
		}
	}

	public DatagramPacket createPacket(byte[] data) {
		return new DatagramPacket(data, data.length, address, port);
	}

	public <T> T remoteCall(RequestResponseDescriptor<T> descriptor) throws Exception {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
				RichDataOutputStream out = new RichDataOutputStream(baos)) {
			out.writeInt(clientNr);
			out.writeInt(++messageNr);
			out.writeInt(descriptor.messageType().ordinal());
			descriptor.buildRequest(out);
			DatagramPacket packet = createPacket(baos.toByteArray());
			sendAndReceivePacket(packet);
			try (RichDataInputStream in = getInputStream()) {
				int answerMessageNr = in.readInt();
				if (answerMessageNr != messageNr) {
					throw new RuntimeException("got a different message nr back than sent!");
				}
				MessageType mt = parseMessageType(in.readInt());
				if (mt == MessageType.NULL) {
					return null;
				} else if (mt == MessageType.EXCEPTION) {
					throw in.readException();
				} else {
					return descriptor.processResponse(in);
				}

			}
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
			throw new UnsupportedOperationException();
		}

		@Override
		public void transfer(bank.Account from, bank.Account to, double amount)
				throws IOException, OverdrawException, InactiveException {
			throw new UnsupportedOperationException();
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
			throw new UnsupportedOperationException();
		}

		@Override
		public void deposit(double amount) throws InactiveException, IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void withdraw(double amount) throws InactiveException, OverdrawException, IOException {
			throw new UnsupportedOperationException();
		}

	}
}