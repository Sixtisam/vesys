package ch.fhnw.ds.udp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

public class UDPClient {

	public static void main(String[] args) throws Exception {
		String host = "localhost";

		try (DatagramSocket socket = new DatagramSocket()) {
			System.out.println("local addr: " + socket.getLocalAddress());
			System.out.println("local port: " + socket.getLocalPort());

			var packet = new DatagramPacket(new byte[0], 0, InetAddress.getByName(host), 4711);

			for (int i = 0; i < 5; i++) {
				var s = String.format("%1$tF %1$tT", LocalDateTime.now());
				packet.setData(s.getBytes(StandardCharsets.US_ASCII));
				socket.send(packet);
				System.out.println("Weg ist es");
				Thread.sleep(1000);
			}
		}
	}

}
