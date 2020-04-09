package bank.ws.server;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.server.Server;

import bank.BankDriver2.UpdateHandler;
import bank.local.Bank;
import bank.tcp.shared.BankAnswer.BankExceptionAnswer;
import bank.tcp.shared.BankCommand;

@ServerEndpoint(
        value = "/bank",
        encoders = { ObjectOutputStreamEncoder.class },
        decoders = { ObjectInputStreamDecoder.class })
public class WsServer implements UpdateHandler {

    public static void main(String[] args) throws DeploymentException, IOException {
        Server server = new Server("localhost", 2222, "", null, WsServer.class);
        server.start();
        System.out.println("Server started, press a key to stop the server");
        System.in.read();
    }

    private final Bank bank;
    private final Set<Session> sessions = ConcurrentHashMap.newKeySet();

    public WsServer() {
        bank = new Bank();
        bank.registerUpdateHandler(this);
    }

    @Override
    public void accountChanged(String id) throws IOException {
        sessions.forEach(session -> {
            try {
                session.getBasicRemote().sendObject(new AccountChangedAnswer(id));
            } catch (Exception e) {
                System.err.println("Failed to notify client " + session.getId() + " for changed account");
                e.printStackTrace();
            }
        });
    }

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("New client connected: " + session.getId());
        sessions.add(session);
    }

    @OnMessage
    public Object onMessage(BankCommand<?> command) {
        try {
            return command.execute(bank);
        } catch (Exception e) {
            return new BankExceptionAnswer(e);
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        System.out.println("Client " + session.getId() + " disconnected: " + closeReason.getReasonPhrase());
        sessions.remove(session);
    }

    @OnError
    public void onError(Throwable exception, Session session) {
        System.err.println("Error for client " + session.getId());
        exception.printStackTrace();
    }
}
