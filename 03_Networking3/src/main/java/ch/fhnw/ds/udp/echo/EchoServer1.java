package ch.fhnw.ds.udp.echo;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;

// this echo server uses the same DatagramPacket for receiving and sending
public class EchoServer1 {

	public static void main(String[] args) throws Exception {
		try (DatagramSocket socket = new DatagramSocket(1234)) {
			System.out.println(socket.getLocalAddress());
			System.out.println(socket.getLocalPort());
			var packet = new DatagramPacket(new byte[0], 0);
			
			byte[] buf = new byte[65535];

			while (true) {
				try {
					// needs a new array to store incoming data because array is set with setData!
					packet.setData(buf);
					socket.receive(packet);
					var msg = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
					var response = String.format("%tT: %s", LocalTime.now(), msg);
					packet.setData(response.getBytes(StandardCharsets.UTF_8));
					socket.send(packet);
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

}
