/*
 * Copyright (c) 2019 Fachhochschule Nordwestschweiz (FHNW)
 * All Rights Reserved. 
 */

package ch.fhnw.ds.compress;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.zip.GZIPOutputStream;

public class CompressServer {

	public static void main(String[] args) throws IOException {
		int port = 55555;
		if (args.length > 0) { 
			port = Integer.parseInt(args[0]);
		}

		try(ServerSocket servSock = new ServerSocket(port)) {
			System.out.println("CompressServer started on port "+port);
	
			while (true) {
				Socket socket = servSock.accept();
				Thread thread = new Thread(new CompressHandler(socket));
				thread.start();
			}
		}
	}
	
	private static class CompressHandler implements Runnable {
		public static final int BUFSIZE = 1024; // Size of receive buffer
		private final Socket socket;

		public CompressHandler(Socket socket) {
			this.socket = socket;
		}

		public static void handleCompressClient(Socket socket) {
			try (InputStream in = socket.getInputStream();
				 GZIPOutputStream out = new GZIPOutputStream(socket.getOutputStream())) {

				byte[] buffer = new byte[BUFSIZE];
				int bytesRead;
				// Receive until client closes connection, indicated by -1 return
				while ((bytesRead = in.read(buffer)) != -1) {
					out.write(buffer, 0, bytesRead);
					System.out.println("Is writing compressed things " + bytesRead);
				}
				System.out.println("Its finished");
			} catch (IOException ex) {
				System.err.println(ex);
			}
			// socket is closed as closing the input stream returned from the socket
			// will close the associated socket.
		}

		public void run() {
			handleCompressClient(this.socket);
		}
	}

}
