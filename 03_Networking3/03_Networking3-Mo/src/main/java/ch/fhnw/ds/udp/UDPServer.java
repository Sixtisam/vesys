package ch.fhnw.ds.udp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class UDPServer {

	public static void main(String[] args) throws Exception {
		try (DatagramSocket socket = new DatagramSocket(4711)) {
			System.out.println(socket.getLocalAddress());
			System.out.println(socket.getLocalPort());
			DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
			// DatagramPacket packet = new DatagramPacket(new byte[1024], 10);
			// DatagramPacket packet = new DatagramPacket(new byte[10], 10);

			while (true) {
				socket.receive(packet);

				InetAddress address = packet.getAddress();
				int port = packet.getPort();
				int len = packet.getLength();
				int offset = packet.getOffset();
				byte[] data = packet.getData();

				System.out.printf("Request from %s:%d of length %d: %s%n", address, port, len,
						new String(data, offset, len, StandardCharsets.US_ASCII));
			}
		}
	}

}
