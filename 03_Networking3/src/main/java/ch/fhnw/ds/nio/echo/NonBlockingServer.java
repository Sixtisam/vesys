package ch.fhnw.ds.nio.echo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class NonBlockingServer {
	public final static int PORT = 4900;

	public static void main(String[] args) throws Exception {
		Selector selector = Selector.open();
		
		ServerSocketChannel serverSocket = ServerSocketChannel.open();
		//serverSocketChannel.socket().bind(new InetSocketAddress(PORT));
		serverSocket.bind(new InetSocketAddress(PORT));
		serverSocket.configureBlocking(false);
		serverSocket.register(selector, SelectionKey.OP_ACCEPT);
		
		while (selector.select() > 0) {
			System.out.println("select returned with " + selector.selectedKeys().size() + " keys");
			
			Iterator<SelectionKey> it = selector.selectedKeys().iterator();
			while (it.hasNext()) {
				SelectionKey key = it.next();
				it.remove();
				handle(key);
			}
		}
	}

	private static void handle(SelectionKey key) throws IOException {
		if (key.isAcceptable()) {
			ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
			SocketChannel socket = ssc.accept();
			socket.configureBlocking(false);
			socket.register(key.selector(), SelectionKey.OP_READ, ByteBuffer.allocate(1024));
			System.out.println("accepted " + socket);
		}

		if (key.isReadable()) {
			System.out.println("isReadable");
			SocketChannel socket = (SocketChannel) key.channel();
			ByteBuffer buffer = (ByteBuffer) key.attachment();
			int n;
			try {
				n = socket.read(buffer);
			} catch(IOException e) {
				n = -1;
			}
			System.out.println(n);
			if (n == -1) {
				socket.close();
			} else {
				if (buffer.hasRemaining()) {
					key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
				} else {
					key.interestOps(SelectionKey.OP_WRITE);
				}
			}
		}

		if (key.isValid() && key.isWritable()) {
			System.out.println("isWriteable");
			SocketChannel socket = (SocketChannel) key.channel();
			ByteBuffer buffer = (ByteBuffer) key.attachment();
			buffer.flip();
			socket.write(buffer);
			if (buffer.hasRemaining()) {
				buffer.compact();
				key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
			} else {
				buffer.clear();
				key.interestOps(SelectionKey.OP_READ);
			}
		}
	}

}
