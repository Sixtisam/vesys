package ch.fhnw.ds.echo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.Socket;

public class EchoClient {

	public static void main(String[] args) throws Exception {
		String host = "localhost";
		int port = 1234;

		try(Socket s = new Socket(host, port, null, 0);
				PrintWriter out = new PrintWriter(s.getOutputStream(), true);
				BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
				BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in))) 
		{
			System.out.println("connected to " + s.getRemoteSocketAddress());
			stdin.lines().takeWhile(line -> !line.equals("")).forEach(line -> {
				try {
					out.println(line);
					System.out.println("Echo: " + in.readLine());
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});
		}	
		System.out.println("disconnected.");
	}
}
