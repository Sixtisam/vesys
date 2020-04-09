package bank.ws.client;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import bank.BankDriver2.UpdateHandler;
import bank.tcp.shared.BankAnswer;
import bank.ws.server.AccountChangedAnswer;
import bank.ws.server.ObjectInputStreamDecoder;
import bank.ws.server.ObjectOutputStreamEncoder;

@ClientEndpoint(
        decoders = { ObjectInputStreamDecoder.class },
        encoders = { ObjectOutputStreamEncoder.class })
public class WsClient {
    private final ArrayBlockingQueue<BankAnswer<?>> answerQueue;
    private final UpdateHandler updateHandler;

    public WsClient(ArrayBlockingQueue<BankAnswer<?>> answerQueue, UpdateHandler updateHandler) {
        super();
        this.answerQueue = answerQueue;
        this.updateHandler = updateHandler;
    }

    @OnOpen
    public void onOpen(Session session) throws IOException {
        System.out.println("Connection to bank server established");
    }

    @OnMessage
    public void onMessage(BankAnswer<?> answer) throws IOException, InterruptedException {
        if (answer instanceof AccountChangedAnswer) {
            updateHandler.accountChanged(((AccountChangedAnswer) answer).getData());
        } else {
            answerQueue.put(answer);
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        System.out.println("Connection to server closed");
    }

    @OnError
    public void onError(Throwable exception, Session session) {
        System.out.println("an error occured on connection " + session.getId() + ":" + exception);
    }
}