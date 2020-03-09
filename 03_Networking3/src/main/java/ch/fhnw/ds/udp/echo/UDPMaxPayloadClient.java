package ch.fhnw.ds.udp.echo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

class UDPMaxPayloadClient {

	public static void main(String[] args) throws Exception {
		String host = "86.119.38.130";
		InetAddress ia = InetAddress.getByName(host);

		try (DatagramSocket socket = new DatagramSocket()) {
			DatagramPacket packet = new DatagramPacket(new byte[512], 512, ia, 1234);
			socket.setSoTimeout(1000);

			try (BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in))) {
				String input = stdin.readLine();
				while (input != null && !input.equals("")) {
					int n = Integer.parseInt(input);
					System.out.println("Sending packet with byte size: " + n);
					byte[] data = new byte[n];
					for (int i = 0; i < n; i++) {
						data[i] = (byte) i;
					}

					packet.setData(data);
					try {
						socket.send(packet);
					} catch (SocketException e) {
						e.printStackTrace();
					}
					try {
						packet.setData(new byte[n]);
						socket.receive(packet);
						System.out.println("Received length of " + packet.getLength());
						System.out.println(packet.getSocketAddress());
					} catch (SocketTimeoutException e) {
						System.out.println("no answer received");
					}
					input = stdin.readLine();
				}
			}
			System.out.println("disconnected.");
		}
	}

}
