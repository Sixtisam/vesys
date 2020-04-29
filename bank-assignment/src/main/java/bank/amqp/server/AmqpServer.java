package bank.amqp.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;

import bank.BankDriver2.UpdateHandler;
import bank.commands.AccountChangedAnswer;
import bank.commands.BankAnswer;
import bank.commands.BankAnswer.BankExceptionAnswer;
import bank.commands.BankCommand;
import bank.local.Bank;

public class AmqpServer implements UpdateHandler {
    public static final String SERVER_QUEUE_NAME = "skeeks.bank.server";
    public static final String SERVER_NOTIFICATIONS_EXCHANGE_NAME = "skeeks.bank.notifications";
    private Connection connection;
    private Channel channel;
    private final Bank bank;

    public static void main(String[] args) throws IOException, TimeoutException {
        AmqpServer server = new AmqpServer();
        server.start();
        System.out.println("Server started, press a key to stop the server");
        System.in.read();
        server.stop();
        System.exit(0);
    }

    public AmqpServer() {
        bank = new Bank();
        bank.registerUpdateHandler(this);
    }

    protected void stop() throws IOException {
        connection.close();
    }

    protected void start() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername("vesys-dev");
        factory.setPassword("vesys123");
        factory.setVirtualHost("vesys-dev-vhost");
        factory.setHost("86.119.38.130");
        factory.setPort(5672);

        connection = factory.newConnection();
        channel = connection.createChannel();

        channel.queueDeclare(SERVER_QUEUE_NAME,
                /* durable: */ false,
                /* exclusive: */ false,
                /* autoDelete: */ false,
                /* arguments: */ null);

        // Notifications will be published on a separate exchange
        channel.exchangeDeclare(SERVER_NOTIFICATIONS_EXCHANGE_NAME, "fanout");

        // queue where commands are received from any clients
        channel.basicConsume(SERVER_QUEUE_NAME, true, (DeliverCallback) this::onMessageReceived, (CancelCallback) null);
    }

    public void accountChanged(String id) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baos);
        out.writeObject(new AccountChangedAnswer(id));
        // publish notification to designated exchange
        channel.basicPublish(SERVER_NOTIFICATIONS_EXCHANGE_NAME, "", null, baos.toByteArray());
    }

    public void onMessageReceived(String consumerTag, Delivery message) throws IOException {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(message.getBody());
            ObjectInputStream ois = new ObjectInputStream(bais);
            Object obj = ois.readObject();
            if (!(obj instanceof BankCommand<?>)) {
                System.err.println("Received unknown object, discarded" + obj.getClass().getName());
            }
            BankCommand<?> command = (BankCommand<?>) obj;

            // process
            System.out.println("Received bank command " + command.getClass().getName());
            BankAnswer<?> answer = processCommand(command);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(answer);

            // the designated queue specified by the sender
            String destQueue = message.getProperties().getReplyTo();
            if (destQueue == null || destQueue.isBlank()) {
                throw new IllegalArgumentException("no reply-to found, therefore cannot send an answer back");
            }
            // send to default exchange, which is a direct queue and therefore will route
            // the message to the 'destQueue' queue
            channel.basicPublish("", destQueue, null, baos.toByteArray());
            System.out.println("Answer is published: " + answer.getClass().getName());
        } catch (ClassNotFoundException ex) {
            throw new IOException(ex);
        }
    }

    public BankAnswer<?> processCommand(BankCommand<?> command) {
        try {
            return command.execute(bank);
        } catch (Exception e) {
            return new BankExceptionAnswer(e);
        }
    }
}
