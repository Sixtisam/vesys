package ch.fhnw.ds.udp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

// https://tools.ietf.org/html/rfc868
public class TimeClient {

	public static void main(String[] args) throws Exception {
		var host = InetAddress.getByName("time.nist.gov");
		int port = 37;
		System.out.println(host);

		try (DatagramSocket socket = new DatagramSocket()) {
			socket.setSoTimeout(1000);
			var request = new DatagramPacket(new byte[0], 0, host, port);

			var data = new byte[1024];
			var response = new DatagramPacket(data, data.length);

			socket.send(request);
			socket.receive(response);
			
			byte[] buf = response.getData();
			long n = (buf[0] + 256) % 256;
			n = (n << 8) + (buf[1] + 256) % 256;
			n = (n << 8) + (buf[2] + 256) % 256;
			n = (n << 8) + (buf[3] + 256) % 256;

			LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochSecond(n - 2_208_988_800L), ZoneId.systemDefault());
			System.out.println("current time: " + date);
		}
	}

}

/*
 * 
 * time.nist.gov/132.163.97.6 37 received -502981438 1970-01-20T01:48:22.210
 * 
 */