package bank.amqp.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;

import bank.BankDriver2;
import bank.BankDriver2.UpdateHandler;
import bank.amqp.server.AmqpServer;
import bank.commands.AccountChangedAnswer;
import bank.commands.BankAnswer;
import bank.commands.BankAnswer.BankExceptionAnswer;
import bank.commands.BankCommand;
import bank.commands.CommandBank;

public class AmqpDriver implements BankDriver2, UpdateHandler {
    private final List<UpdateHandler> updateHandlers = new ArrayList<>();

    /**
     * holds all answers retrieved from server but not yet processed.
     */
    private final BlockingQueue<BankAnswer<?>> receivedAnswers = new SynchronousQueue<>();

    private Connection connection;
    private Channel channel;

    /**
     * The queue name where I expect the server's BankAnswer<?> objects
     */
    private String answerQueueName;
    private Bank bank;

    @Override
    public void connect(String[] args) throws IOException {
        bank = new Bank();
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername("vesys-dev");
        factory.setPassword("vesys123");
        factory.setVirtualHost("vesys-dev-vhost");
        factory.setHost("86.119.38.130");
        factory.setPort(5672);

        try {
            connection = factory.newConnection();
        } catch (TimeoutException e) {
            throw new IOException(e);
        }
        channel = connection.createChannel();

        // Commands/Answers:

        // Declare my client queue, where I receive answers from the server
        answerQueueName = channel.queueDeclare().getQueue();
        // register consumer for my queue
        channel.basicConsume(answerQueueName, true, (DeliverCallback) this::onMessageDelivered, (CancelCallback) null);

        // Notifications (handled in a separate queue):

        // declare the server notifications exchange
        channel.exchangeDeclare(AmqpServer.SERVER_NOTIFICATIONS_EXCHANGE_NAME, "fanout");
        // declare my notification queue (without a specific name
        String myNotificationsQueue = channel.queueDeclare().getQueue();
        channel.basicConsume(myNotificationsQueue, (DeliverCallback) this::onNotificationDelivered, (CancelCallback) null);
        // bind my own notification queue to the server notifications exchange
        channel.queueBind(myNotificationsQueue, AmqpServer.SERVER_NOTIFICATIONS_EXCHANGE_NAME, "");

    }

    /**
     * Called whenever a notification is received from the server
     */
    public void onNotificationDelivered(String consumer, Delivery message) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(message.getBody());
            ObjectInputStream ois = new ObjectInputStream(bais);
            Object obj = ois.readObject();
            if (obj instanceof AccountChangedAnswer) {
                System.out.println("Received account changed answer");
                AccountChangedAnswer answer = (AccountChangedAnswer) obj;
                // inform listeners
                accountChanged(answer.getData());
            } else {
                System.err.println("unknown notification data received, discarded.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void onMessageDelivered(String consumer, Delivery message) {
        try {
        	// XXX das Konvertieren eines byte[] in ein BankCommand/BankAnswer oder umgekehrt taucht immer wieder auf,
        	//     und es würde sich wohl lohnen, diese Zeilen in eine separate Methode zu verschieben.
            ByteArrayInputStream bais = new ByteArrayInputStream(message.getBody());
            ObjectInputStream ois = new ObjectInputStream(bais);
            Object obj = ois.readObject();
            if (obj instanceof BankAnswer<?>) {
                System.out.println("Received bank answer " + obj.getClass().getName());
                BankAnswer<?> answer = (BankAnswer<?>) obj;
                receivedAnswers.put(answer);
            } else {
                System.err.println("unknown data received on client queue, discard");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void disconnect() throws IOException {
        connection.close();
        bank = null;
    }

    @Override
    public void registerUpdateHandler(UpdateHandler handler) throws IOException {
        updateHandlers.add(handler);
    }

    /**
     * informs all listeners about the changed account
     */
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
    public Bank getBank() {
        return bank;
    }

    public class Bank extends CommandBank {

        @Override
        protected <T extends BankCommand<? extends R>, R extends BankAnswer<? extends Serializable>> R remoteCall(
                T cmd, Class<R> resultType) throws IOException, Exception {

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(cmd);

            // tell the server with reply-to where to put the answer
            BasicProperties props = new BasicProperties.Builder().replyTo(answerQueueName).build();
            // publish command to server
            channel.basicPublish("", AmqpServer.SERVER_QUEUE_NAME, props, baos.toByteArray());
            System.out.println("Command is published to server");

            // waits until answer is received.
            BankAnswer<?> answer = receivedAnswers.take();
            // decide if exception or valid result
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
