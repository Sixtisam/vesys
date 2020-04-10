package bank.ws.client;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import javax.websocket.Session;

import org.glassfish.tyrus.client.ClientManager;

import bank.BankDriver2;
import bank.BankDriver2.UpdateHandler;
import bank.commands.BankAnswer;
import bank.commands.BankCommand;
import bank.commands.CommandBank;
import bank.commands.BankAnswer.BankExceptionAnswer;

public class WsDriver implements BankDriver2, UpdateHandler {
    private Session session;
    private Bank bank;
    private final List<UpdateHandler> updateHandlers = new ArrayList<>();
    private final ArrayBlockingQueue<BankAnswer<?>> answerQueue = new ArrayBlockingQueue<>(10);

    @Override
    public void connect(String[] args) throws IOException {
        try {
            bank = new Bank();
            URI url = new URI("ws://" + args[0] + ":" + args[1] + "/ws/bank");
            ClientManager client = ClientManager.createClient();
            session = client.connectToServer(new WsClient(answerQueue, this), url);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("failed to establish connection", e);
        }
    }

    public void registerUpdateHandler(UpdateHandler handler) {
        updateHandlers.add(handler);
    }

    protected void notify(String number) {
        updateHandlers.forEach(uH -> {
            try {
                uH.accountChanged(number);
            } catch (IOException e) {
                System.err.println("UpdateHandler throwed exception " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void accountChanged(String number) {
        updateHandlers.forEach(uH -> {
            try {
                uH.accountChanged(number);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void disconnect() throws IOException {
        session.close();
    }

    @Override
    public Bank getBank() {
        return bank;
    }

    public class Bank extends CommandBank {

        @Override
        protected <T extends BankCommand<? extends BankAnswer<? extends Serializable>>, R extends BankAnswer<? extends Serializable>> R remoteCall(
                T cmd, Class<R> resultType) throws IOException, Exception {
            session.getBasicRemote().sendObject(cmd);
            BankAnswer<?> answer = answerQueue.take();

            if (answer instanceof BankExceptionAnswer) {
                throw ((BankExceptionAnswer) answer).getData();
            } else if (answer instanceof BankAnswer) {
                return resultType.cast(answer);
            } else {
                throw new IOException("unexpected type " + answer.getClass().getSimpleName());
            }
        }
    }
}
