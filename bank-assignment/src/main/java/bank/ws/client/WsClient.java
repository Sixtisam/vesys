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
import bank.commands.AccountChangedAnswer;
import bank.commands.BankAnswer;
import bank.ws.server.ObjectInputStreamDecoder;
import bank.ws.server.ObjectOutputStreamEncoder;

@ClientEndpoint(
        decoders = { ObjectInputStreamDecoder.class },
        encoders = { ObjectOutputStreamEncoder.class })
public class WsClient {
    /**
     * contains all received answers that are not yet processed
     */
    private final ArrayBlockingQueue<BankAnswer<?>> answerQueue;
    /**
     * the handle informed by incoming {@link AccountChangedAnswer}'s
     */
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
    public void onMessage(Object obj) throws IOException, InterruptedException {
        BankAnswer<?> answer = (BankAnswer<?>) obj;
        
        // branch between AccountChanged notification and other answers
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
        System.out.println("an error occured " + session.getId() + ":" + exception);
        exception.printStackTrace();
    }
}