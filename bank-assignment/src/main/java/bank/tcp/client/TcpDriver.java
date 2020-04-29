package bank.tcp.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.net.UnknownHostException;

import bank.commands.BankAnswer;
import bank.commands.BankAnswer.BankExceptionAnswer;
import bank.commands.BankCommand;
import bank.commands.CommandBank;

public class TcpDriver implements bank.BankDriver {
    private Bank bank = null;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    @Override
    public void connect(String[] args) throws UnknownHostException, IOException {
        bank = new Bank();
        System.out.println("connected...");
        socket = new Socket(args[0], Integer.parseInt(args[1]));
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    @Override
    public void disconnect() throws IOException {
        bank = null;
        out.close();
        if (!socket.isClosed()) {
            throw new RuntimeException("Not closed");
        }
        System.out.println("disconnected...");
    }

    @Override
    public bank.Bank getBank() {
        return bank;
    }

    public class Bank extends CommandBank {

        @Override
        protected <T extends BankCommand<? extends R>, R extends BankAnswer<? extends Serializable>> R remoteCall(
                T cmd, Class<R> resultType) throws IOException, Exception {
            try {
                out.writeObject(cmd);
                Object obj = in.readObject();
                if (obj instanceof BankExceptionAnswer) {
                    throw ((BankExceptionAnswer) obj).getData();
                } else if (obj instanceof BankAnswer) {
                    return resultType.cast(obj);
                } else {
                    throw new IOException("unexpected type " + obj.getClass().getSimpleName());
                }
            } catch (ClassNotFoundException e) {
                throw new IOException("unable to deserialize: class not found", e);
            }
        }
    }
}