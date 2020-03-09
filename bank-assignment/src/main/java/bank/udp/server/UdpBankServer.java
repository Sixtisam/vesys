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

import bank.udp.server.RequestHandler.CreateAccountRequestHandler;
import bank.udp.server.RequestHandler.GetAccountBalanceRequestHandler;
import bank.udp.server.RequestHandler.GetAccountNumbersRequestHandler;
import bank.udp.server.RequestHandler.GetAccountOwnerRequestHandler;
import bank.udp.server.RequestHandler.ResultIsNullException;
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
		fillRequestHandlerMap();
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

	public static void fillRequestHandlerMap() {
		HANDLER_MAP.put(MessageType.GET_ACCOUNT_NUMBERS, new GetAccountNumbersRequestHandler());
		HANDLER_MAP.put(MessageType.CREATE_ACCOUNT, new CreateAccountRequestHandler());
		HANDLER_MAP.put(MessageType.GET_OWNER, new GetAccountOwnerRequestHandler());
		HANDLER_MAP.put(MessageType.GET_BALANCE, new GetAccountBalanceRequestHandler());
	}

	/**
	 * Is responsible for handling requests from a specific user.
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
				// in case of hello packet
				if (packet.getLength() == 0) {
					System.out.println("Hello Paket detected");
					out.writeInt(CLIENT_NR_SEQUENCE.incrementAndGet());
					data = baos.toByteArray();
				} else {
					data = handleRequest(in, baos, out);
					System.out.println("Handled request");
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
			int messageNr = in.readInt();
			byte[] data = REQUEST_RESPONSE_LOG.getOrDefault(Integer.valueOf(clientNr), Collections.emptyMap())
					.get(messageNr);

			if (data != null) {
				return data;
			}

			try {
				out.writeInt(messageNr);
				out.writeInt(MessageType.RESPONSE.ordinal());
				handleRequest(in, out);
			} catch (ResultIsNullException ex) {
				baos.reset();
				out.writeInt(messageNr);
				out.writeInt(MessageType.NULL.ordinal());
			} catch (Exception e) {
				// when handler throwed, reset data and write the exception to the stream.
				baos.reset();
				out.writeInt(messageNr);
				out.writeInt(MessageType.EXCEPTION.ordinal());
				out.writeException(e);
			}
			data = baos.toByteArray();
			REQUEST_RESPONSE_LOG.computeIfAbsent(Integer.valueOf(clientNr), k -> new HashMap<>()).put(messageNr, data);
			return data;
		}

		protected void handleRequest(RichDataInputStream in, RichDataOutputStream out) throws Exception {
			// resend packet if client has already submitted the same requested (IDEMPOTENZ)
			int messageType = in.readInt();
			MessageType type = MessageType.values()[messageType];
			RequestHandler requestHandler = HANDLER_MAP.get(type);
			requestHandler.handle(BANK, in, out);
		}
	}
}
