package ch.fhnw.ds.nio.echo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

public class NonBlockingClient {
	private static SocketChannel socket = null;

	public static void main(String[] args) throws Exception {
		socket = SocketChannel.open();
		socket.connect(new InetSocketAddress("localhost", 4900));
		socket.configureBlocking(false);

		Thread t = new Thread(() -> {
			ByteBuffer buf = ByteBuffer.allocate(2048);
			try {
				while (true) {
					while (socket.read(buf) > 0) {
						buf.flip();
						Charset charset = Charset.forName("us-ascii");
						CharsetDecoder decoder = charset.newDecoder();
						CharBuffer charBuffer = decoder.decode(buf);
						String result = charBuffer.toString();
						System.out.println(result);
						buf.clear();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		t.start();

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			String msg = in.readLine();
			ByteBuffer bytebuf = ByteBuffer.wrap(msg.getBytes());
			int nBytes = socket.write(bytebuf);
			if (msg.equals("quit") || msg.equals("shutdown")) {
				System.out.println("stop the client");
				socket.close();
				break;
			}
			System.out.println("Wrote " + nBytes + " bytes to the server");
		}

	}

}
