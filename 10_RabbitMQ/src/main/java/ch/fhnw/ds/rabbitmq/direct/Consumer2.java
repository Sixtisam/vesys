package ch.fhnw.ds.rabbitmq.direct;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class Consumer2 {

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

		channel.basicQos(1);
		boolean autoAck = false;

		DeliverCallback deliverCallback = (consumerTag, message) -> {
			String text = new String(message.getBody(), StandardCharsets.UTF_8);
			System.out.println(" [x] Received '" + text + "'");
//			sleep(1000);
			System.out.println("aborting");
			throw new RuntimeException("sddxc");
//			System.out.println("done");
//			channel.basicAck(message.getEnvelope().getDeliveryTag(), true);
		};
		CancelCallback cancelCallback = consumerTag -> {
			System.out.println("Cancelled by the server");
			System.out.println("Consumer Tag: " + consumerTag);
		};
		String tag = channel.basicConsume(QUEUE_NAME, autoAck, deliverCallback, cancelCallback);
		System.out.println(tag);
	}

	private static void sleep(int seconds) throws IOException {
		try {
			Thread.sleep(seconds * 1000);
		} catch (InterruptedException e) {
			throw new IOException(e);
		}
	}
}