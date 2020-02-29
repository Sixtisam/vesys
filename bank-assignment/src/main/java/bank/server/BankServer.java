package bank.server;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import bank.shared.BankAnswer;
import bank.shared.BankAnswer.BankExceptionAnswer;
import bank.shared.BankCommand;

public class BankServer {
	public static void main(String[] args) {
		System.out.println("Initializing bank server...");
		ExecutorService svc = Executors.newCachedThreadPool();
		ServerBank bank = new ServerBank();
		try (ServerSocket server = new ServerSocket(1234)) {
			while (true) {
				System.out.println("Waiting for client...");
				svc.execute(new ClientHandler(server.accept(), bank));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Is responsible for handling requests from a specific user.
	 *
	 */
	public static class ClientHandler implements Runnable {
		private Socket socket;
		private ServerBank bank;

		public ClientHandler(Socket socket, ServerBank bank) {
			this.socket = socket;
			this.bank = bank;
		}

		@Override
		public void run() {
			System.out.println("Client thread exeucting...");
			try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
				try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
					while (true) {
						Object obj = in.readObject();
						if (obj instanceof BankCommand) {
							out.writeObject(processCommand((BankCommand<?>) obj));
						} else {
							System.err.println("Ignore non-bank command" + obj.getClass().getSimpleName());
						}
					}
				} finally {
					// nop
				}
			} catch (Exception e) {
				System.err.println("Unexpected exception in client handler");
				e.printStackTrace();
			}
		}

		/**
		 * Processes the passed command. In case the command's <code>execute</code>
		 * method throws an Exception, that exception is wrapped in a
		 * <code>BankExceptionAnswer</code> object and returned.
		 */
		public BankAnswer<?> processCommand(BankCommand<?> command) {
			try {
				return command.execute(bank);
			} catch (Exception e) {
				return new BankExceptionAnswer(e);
			}
		}

	}
}
