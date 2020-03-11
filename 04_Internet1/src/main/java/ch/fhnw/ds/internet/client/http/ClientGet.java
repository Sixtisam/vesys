package ch.fhnw.ds.internet.client.http;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ClientGet {
	
	public static void main(String[] args) throws Exception {
		HttpClient client = HttpClient.newHttpClient();

		// the following lines create a HttpClient which follows redirects
		// client = HttpClient.newBuilder().followRedirects(Redirect.NORMAL).build();
		
		HttpRequest request = HttpRequest.newBuilder()
				.uri(new URI("http://www.fhnw.ch/de"))
				.GET()
				.build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

		System.out.println("Statuscode: " + response.statusCode());
		System.out.println("Headers:");
		response.headers().map().forEach((k, v) -> System.out.println(k + ": " + v));
		System.out.println("Body:");
		System.out.println(response.body());
	}

}

