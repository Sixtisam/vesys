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

import bank.commands.AccountChangedAnswer;
import bank.commands.BankCommand;
import bank.commands.BankAnswer.BankExceptionAnswer;
import bank.local.Bank;

@ServerEndpoint(
        value = "/bank",
        encoders = { ObjectOutputStreamEncoder.class },
        decoders = { ObjectInputStreamDecoder.class })
public class WsServer {

    private static final Bank bank;

    /**
     * Sessions are stored separately instead of using Session.getOpenSession() 
     */
    private static final Set<Session> sessions = ConcurrentHashMap.newKeySet();

    public static void main(String[] args) throws DeploymentException, IOException {
        Server server = new Server("localhost", 2222, "/ws", null, WsServer.class);
        server.start();
        System.out.println("Server started, press a key to stop the server");
        System.in.read();
    }

    static {
        bank = new Bank();
        bank.registerUpdateHandler(WsServer::accountChanged);
    }

    public static void accountChanged(String id) throws IOException {
        sessions.forEach(session -> {
            if (session.isOpen()) {
                try {
                    // notify all sessions about the changed account
                    session.getBasicRemote().sendObject(new AccountChangedAnswer(id));
                } catch (Exception e) {
                    System.err.println("Failed to notify client " + session.getId() + " for changed account");
                    e.printStackTrace();
                }
            }
        });
    }

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("New client connected: " + session.getId());
        sessions.add(session);
    }

    @OnMessage
    public Object onMessage(Object obj) {
        // Object param is required because it must corresponds to the generic type of
        // ObjectInputStreamDecoder
    	// XXX Genau. Variante wäre unterschiedliche Encodeer/Decoder auf Client und Serverseite, dann könnte man BankCoomand als Parametertp verwenden.
        BankCommand<?> command = (BankCommand<?>) obj;
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
