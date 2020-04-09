package bank.ws.client;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import javax.websocket.Session;

import org.glassfish.tyrus.client.ClientManager;

import bank.Bank;
import bank.BankDriver2;
import bank.BankDriver2.UpdateHandler;
import bank.tcp.shared.BankAnswer;

public class WsDriver implements BankDriver2, UpdateHandler {
    private Session session;
    private final List<UpdateHandler> updateHandlers = new ArrayList<>();
    private final ArrayBlockingQueue<BankAnswer<?>> answerQueue = new ArrayBlockingQueue<>(10);

    @Override
    public void connect(String[] args) throws IOException {
        try {
            URI url = new URI("ws://localhost:2222/websockets/bank");
            ClientManager client = ClientManager.createClient();
            session = client.connectToServer(new WsClient(answerQueue, this), url);
        } catch (Exception e) {
            e.printStackTrace();
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
        // TODO use same bank as in tcp
        return null;
    }
}
