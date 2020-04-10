package bank.tcp.server;

import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import bank.commands.BankAnswer;
import bank.commands.BankCommand;
import bank.commands.BankAnswer.BankExceptionAnswer;
import bank.local.Bank;

public class TcpServer {
    public static void main(String[] args) {
        System.out.println("Initializing bank server...");
        ExecutorService svc = Executors.newCachedThreadPool();
        Bank bank = new Bank();
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
        private final Socket socket;
        private final Bank bank;

        public ClientHandler(Socket socket, Bank bank) {
            this.socket = socket;
            this.bank = bank;
        }

        @Override
        public void run() {
            System.out.println("Client thread exeucting...");
            try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                while (true) {
                    Object obj = in.readObject();
                    if (obj instanceof BankCommand) {
                        out.writeObject(processCommand((BankCommand<?>) obj));
                    } else {
                        System.err.println("Ignore non-bank command" + obj.getClass().getSimpleName());
                    }
                }
            } catch (EOFException ex) {
                System.out.println("Client closed connection");
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
