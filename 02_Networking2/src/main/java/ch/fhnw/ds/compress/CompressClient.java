/*
 * Copyright (c) 2019 Fachhochschule Nordwestschweiz (FHNW)
 * All Rights Reserved. 
 */

package ch.fhnw.ds.compress;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Random;

public class CompressClient {

	public static final int BUFSIZE = 256; // Size of read buffer

	public static final String FILE_NAME = "Test"; // name of the generated file
	public static final int FILE_SIZE = 20000; // size of the generated file in KBtyes

	public static void main(String[] args) throws Exception {
		String server = "localhost";
		int port = 55555;
		if (args.length > 0) {
			server = args[0];
		}
		if (args.length > 1) {
			port = Integer.parseInt(args[1]);
		}

		// create a file with a given size
		createFile(FILE_NAME, FILE_SIZE * 1024);

		// Open input and output file (named input.gz)
		final FileInputStream fileIn = new FileInputStream(FILE_NAME);
		final FileOutputStream fileOut = new FileOutputStream(FILE_NAME + ".gz");

		// Create socket connected to server on specified port
		final Socket sock = new Socket(server, port);

		// Send uncompressed byte stream to server
		// Lösung:
		new Thread(() -> {
			try {
				sendBytes(sock, fileIn); // Bereits vorher da
			} catch (IOException e) {
				e.printStackTrace();
			}
		}).start();
		// Lösung ende

		// Receive compressed byte stream from server
		InputStream sockIn = sock.getInputStream();
		int bytesRead;
		byte[] buffer = new byte[BUFSIZE];
		while ((bytesRead = sockIn.read(buffer)) != -1) {
			fileOut.write(buffer, 0, bytesRead);
			Log('R'); // Reading progress indicator
		}
		System.out.println(); // End progress indicator line

		sock.close();
		fileIn.close();
		fileOut.close();
	}

	private static void createFile(String name, int size) throws Exception {
		Random r = new Random();
		OutputStream out = new FileOutputStream(new File(name));
		byte[] buf = new byte[8];
		int pos = 0;
		while (pos < size) {
			Arrays.fill(buf, (byte) r.nextInt());
			out.write(buf, 0, Math.min(8, size - pos));
			pos += 8;
		}
		out.close();
	}

	private static void sendBytes(Socket sock, InputStream fileIn) throws IOException {
		OutputStream sockOut = sock.getOutputStream();
		int bytesRead;
		byte[] buffer = new byte[BUFSIZE];
		while ((bytesRead = fileIn.read(buffer)) != -1) {
			sockOut.write(buffer, 0, bytesRead);
			Log('W'); // Writing progress indicator
		}
		System.out.println("FInished");
		sock.shutdownOutput();
		System.out.println(); // End progress indicator line
	}

	private static int c = 0;

	static synchronized void Log(char s) {
		System.out.print(s);
		if (++c % 100 == 0)
			System.out.println();
	}
}
