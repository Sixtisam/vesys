package ch.fhnw.ds.internet.server.web;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.SSLServerSocketFactory;

/**
 * The root directory of the server is the directory in which the server was started.
 * 
 * Start with options 8080 false or 443 true
 * 
 * Source: Go To Java2
 */
public class Server {
	
	private static final int SOCKET_TIMEOUT = 30000;

	public static void main(String args[]) throws Exception {
		int port;
		try {
			port = Integer.parseInt(args[0]);
		} catch (Exception e) {
			port = 8080;
		}
		
		boolean secure;
		try {
			secure = Boolean.valueOf(args[args.length - 1]).booleanValue();
		} catch (Exception e) {
			secure = false;
		}

		System.out.print("WebServer listening at port " + port);
		if(secure) { System.out.print(" [https]"); }
		System.out.println();
		
		ServerSocket ss;
		if (secure) {
			SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
			ss = ssf.createServerSocket(port);
		} else {
			ss = new ServerSocket(port);
		}

		int calls = 0;
		try (ss) {
			while (true) {
				Socket s = ss.accept();
				s.setSoTimeout(SOCKET_TIMEOUT);
				new BrowserClientThread(++calls, s).start();
			}
		}
	}
}

class BrowserClientThread extends Thread {
	
	private final Socket socket;
	private final int id;

	// set by method readRequest
	private String cmd = "";
	private String url = "";
	private String httpversion = "";
	private final Map<String, String> headers = new HashMap<>();
	
	private int contentLength = 0;

	public BrowserClientThread(int id, Socket socket) {
		this.id = id;
		this.socket = socket;
	}

	@Override
	public void run() {
		System.out.println(id + ": Incoming call...");
		try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			 BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {
			readRequest(in);
			writeResponse(in, out);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(id + ": " + e.toString());
		} finally {
			System.out.println(id + ": Closed.");
		}
	}

	private void readRequest(BufferedReader in) throws IOException {
		String requestline = in.readLine();
		System.out.println(id + ": > " + requestline);
		
		if (requestline == null || requestline.length() == 0) 
			throw new IllegalArgumentException();
		
		var req = requestline.split(" ");
		cmd = req[0];
		url = req[1];
		httpversion = req[2];

		String line = in.readLine();
		while (line != null && line.length() > 0) {
			int pos = line.indexOf(':');
			String key = line.substring(0, pos);
			String value = line.substring(pos+1);
			headers.put(key.toUpperCase(), value.trim());
			line = in.readLine();
		}
		
		if(headers.containsKey("CONTENT-LENGTH")) {
			contentLength = Integer.parseInt(headers.get("CONTENT-LENGTH").trim());
		}
	}

	private void writeResponse(BufferedReader in, BufferedWriter out) throws IOException {
		if (cmd.equals("GET")) {
			if(url.startsWith("/echo")) {
				out.write("HTTP/1.0 200 OK\r\n");
				out.write("Server: WebServer 0.5\r\n");
				out.write("Content-type: text/plain\r\n\r\n");
				out.write(cmd + " " + url + " " + httpversion + "\r\n\r\n");
				out.write("Headers:\r\n");
				for(Entry<String, String> e : headers.entrySet()) {
					out.write(e.getKey() + ": " + e.getValue() + "\r\n");
				}
				return;
			}
			
			if (!url.startsWith("/")) {
				httpError(out, 400, "Bad Request");
			} else {
				String contentType;
				try {
					java.nio.file.Path path = Paths.get(url);
					contentType = Files.probeContentType(path);
					if(contentType == null) contentType = "application/octet-stream";
				} catch(Exception e) {
					contentType = "application/octet-stream";
				}

				//convert URL to filename
				String fsep = System.getProperty("file.separator", "/");
				String filename = url.substring(1).replace("/", fsep);

				File f = null;
				try {
					f = new File(filename);
					// if file has not been found, then look in the src directory
					if (!f.exists()) {
						f = new File("src" + fsep + "main" + fsep + "java" + fsep + filename);
					}
					// if file has not been found, then look in the Eclipse bin directory
					if (!f.exists()) {
						f = new File("bin" + fsep + filename);
					}
					// if file has not been found, then look in the Gradle  bin directory
					if (!f.exists()) {
						f = new File("build" + fsep + "classes" + fsep + "java" + fsep + "main" + fsep + filename);
					}
					// if file has not been found, then look in the web directory
					if (!f.exists()) {
						f = new File("src" + fsep + "main" + fsep + "resources" + fsep + "web" + fsep + filename);
					}
				} catch(Exception e) {
					
				}

				try (FileInputStream is = new FileInputStream(f)) {
					// send HTTP header
					out.write("HTTP/1.0 200 OK\r\n");
					out.write("Server: WebServer 0.5\r\n");
					out.write("Content-type: " + contentType + "\r\n\r\n");
					out.flush();
					System.out.println(id + ": < HTTP/1.0 200 OK");
					System.out.println(id + ": < Server: WebServer 0.5");
					System.out.println(id + ": < Content-type: " + contentType);

					// send HTTP body (i.e. file content)
					is.transferTo(socket.getOutputStream());
				} catch (IOException e) {
					e.printStackTrace();
					httpError(out, 404, "File not found");
				} catch (Exception e) {
					e.printStackTrace();
					httpError(out, 500, "Unknown exception");
				}
			}
		} else if(cmd.equals("POST")){
			System.out.println("POST BODY ("+contentLength+")");
			if(url.startsWith("/echo")) {
				out.write("HTTP/1.0 200 OK\r\n");
				out.write("Server: WebServer 0.5\r\n");
				out.write("Content-type: text/plain\r\n\r\n");
				out.write(cmd + " " + url + " " + httpversion + "\r\n\r\n");
				out.write("Headers:\r\n");
				for(Entry<String, String> e : headers.entrySet()) {
					out.write(e.getKey() + ": " + e.getValue() + "\r\n");
				}
				out.write("\r\n");
				out.write("Body:\r\n");
				out.flush();
				
				int c = in.read();
				int pos = 0;
				while (c != -1) {
					out.write(c);
					pos++;
					if(pos == contentLength) break;
					c = in.read();
				}
				return;
			} else {
				try {
					int c = in.read();
					int pos = 0;
					while (c != -1 && pos < contentLength && pos < 1000) {
						System.out.print((char) c);
						pos++;
						if(pos == contentLength) break;
						c = in.read();
					}
					if (pos < contentLength) System.out.print("...");
					System.out.println();
				} catch (IOException e) {
				}
				System.out.println();
				httpError(out, 501, "POST not implemented");
			}
		}
		else {
			httpError(out, 501, "Not implemented");
		}
	}
		
	/**
	 * Eine Fehlerseite an den Browser senden.
	 */
	private void httpError(BufferedWriter out, int code, String description) throws IOException {
		System.out.println(description);
		out.write("HTTP/1.0 " + code + " " + description + "\r\n");
		out.write("Content-type: text/html\r\n\r\n");
		out.write("<html>\r\n");
		out.write("<head>\r\n");
		out.write("<title>WebServer-Error</title>\r\n");
		out.write("</head>\r\n");
		out.write("<body>\r\n");
		out.write("<h1>HTTP/1.0 " + code + "</h1>\r\n");
		out.write("<h3>" + description + "</h3>\r\n");
		out.write("</body>\r\n");
		out.write("</html>\r\n");
	}
}