package ch.fhnw.ds.udp.echo;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;

// this echo server creates a new DatagramPacket for each response
public class EchoServer2 {

	public static void main(String[] args) throws Exception {
		System.out.println("EchoServer2 started");
		try (DatagramSocket socket = new DatagramSocket(1234)) {
			System.out.println(socket.getLocalAddress());
			System.out.println(socket.getLocalPort());
			var packet = new DatagramPacket(new byte[65535], 65535);
			
			while (true) {
				packet.setLength(65533);
				socket.receive(packet);
				var msg = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
				var response = String.format("%tT: %s", LocalTime.now(), msg);
				byte[] buf = response.getBytes(StandardCharsets.UTF_8);
				var resp = new DatagramPacket(buf, buf.length, packet.getSocketAddress());
				socket.send(resp);
			}
		}
	}

}
