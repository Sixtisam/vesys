package ch.fhnw.ds.rabbitmq.direct;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class Producer {

	final static String QUEUE_NAME = "hello_skeeks";

	public static void main(String[] argv) throws Exception {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setUsername("vesys-dev"); // Default: guest (only works on localhost)
		factory.setPassword("vesys123"); // Default: guest (only works on localhost)
		factory.setVirtualHost("vesys-dev-vhost"); // Default: /
		factory.setHost("86.119.38.130"); // Default: localhost
		factory.setPort(5672); // Default: 5672

		try (Connection connection = factory.newConnection();
				Channel channel = connection.createChannel()) {

			channel.queueDeclare(QUEUE_NAME, 
					/* durable:    */ false, 
					/* exclusive:  */ false,
					/* autoDelete: */ false,
					/* arguments:  */ null);
			
			String message = "Hello World at " + LocalDateTime.now();
			System.out.println(message);
			channel.basicPublish(
					/* exchange:    */ "",			// Exchange: empty string is called "default exchange" which is a direct exchange. 
					/* routing key: */ QUEUE_NAME,
					/* props:       */ null,		// MessageProperties
					/* body:        */ message.getBytes(StandardCharsets.UTF_8));

			System.out.println(" [x] Sent '" + message + "'");
		}
	}
}