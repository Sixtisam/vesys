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

public class EchoServer1 {

	public static void main(String args[]) throws IOException {
		try (ServerSocket server = new ServerSocket(1234, 2)) {
			System.out.println("Startet Echo Server on port " + server.getLocalPort());
			while (true) {
				try (var s = server.accept()) {
					BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
					PrintWriter out = new PrintWriter(s.getOutputStream(), true);

					in.lines().forEach(line -> {
						out.println(line);
						System.out.println(">> " + line);
					});
					System.out.println("done serving " + s);
				}
			}
		}
	}
}