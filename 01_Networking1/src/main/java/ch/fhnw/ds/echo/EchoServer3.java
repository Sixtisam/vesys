/*
 * Copyright (c) 2020 Fachhochschule Nordwestschweiz (FHNW)
 * All Rights Reserved. 
 */

package ch.fhnw.ds.echo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;

public class EchoServer3 {

	private static int SIZE_THREAD_POOL = 4;

	public static void main(String args[]) throws IOException {
		int port = 1234;
		@SuppressWarnings("resource")
		ServerSocket server = new ServerSocket(port);
		System.out.println("Startet Echo Server on port " + port);
		for (int i = 0; i < SIZE_THREAD_POOL; i++) {
			Thread t = new Thread(() -> {
				while (true) {
					try (var s = server.accept()) {
						System.out.println("Local port is " + s.getLocalPort());
						System.out.println("connection from " + s);

						var in = new BufferedReader(new InputStreamReader(s.getInputStream()));
						var out = new PrintWriter(s.getOutputStream(), true);

						in.lines().forEach(line -> {
							out.println(line);
							System.out.println(">> " + line);
						});

						System.out.println("done serving " + s);
					} catch (IOException e) {
						System.err.println(e);
					}
				}
			});
			t.start();
		}
	}
}
