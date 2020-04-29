package ch.fhnw.ds.rabbitmq.direct;

import java.nio.charset.StandardCharsets;

import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class Consumer {

    private final static String QUEUE_NAME = Producer.QUEUE_NAME;

	public static void main(String[] argv) throws Exception {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setUsername("vesys-dev");
		factory.setPassword("vesys123");
		factory.setVirtualHost("vesys-dev-vhost");
		factory.setHost("86.119.38.130");
		factory.setPort(5672);

		Connection connection = factory.newConnection();
		Channel channel = connection.createChannel();

		channel.queueDeclare(QUEUE_NAME, 
				/* durable:    */ false, 
				/* exclusive:  */ false,
				/* autoDelete: */ false,
				/* arguments:  */ null);
		System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

		DeliverCallback deliverCallback = (consumerTag, message) -> {
			String text = new String(message.getBody(), StandardCharsets.UTF_8);
			System.out.println(" [x] Received '" + text + "'");
		};
		CancelCallback cancelCallback = consumerTag -> {
			System.out.println("Cancelled by the server");
			System.out.println("Consumer Tag: " + consumerTag);
		};
		String tag = channel.basicConsume(QUEUE_NAME, true, deliverCallback, cancelCallback);
		System.out.println(tag);
	}
}
