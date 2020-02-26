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
import java.net.Socket;

public class EchoServer2 {

	public static void main(String args[]) throws IOException {
		int port = 1234;
		try (ServerSocket server = new ServerSocket(port)) {
			System.out.println("Startet Echo Server on port " + port);
			while (true) {
				Socket s = server.accept();
				Thread t = new Thread(() -> {
					try (var in = new BufferedReader(new InputStreamReader(s.getInputStream()));
							var out = new PrintWriter(s.getOutputStream(), true)) {
						System.out.println("connection from " + s);
						
						in.lines().forEach(line -> {
							out.println(line);
							System.out.println(">> " + line);
						});

						System.out.println("done serving " + s);
						s.close();
					} catch (IOException e) {
						System.err.println(e);
						throw new RuntimeException(e);
					}
				});
				t.start();
			}
		}
	}

}
