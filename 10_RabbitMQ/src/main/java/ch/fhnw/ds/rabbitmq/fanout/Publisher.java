package ch.fhnw.ds.rabbitmq.fanout;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class Publisher {

	static final String EXCHANGE_NAME = "logs";

	public static void main(String[] argv) throws Exception {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setUsername("vesys-dev");
		factory.setPassword("vesys123");
		factory.setVirtualHost("vesys-dev-vhost");
		factory.setHost("86.119.38.130");
		factory.setPort(5672);

		try (Connection connection = factory.newConnection(); 
				Channel channel = connection.createChannel()) {

			channel.exchangeDeclare(EXCHANGE_NAME, "fanout");

			String message = "Current Date: " + LocalDateTime.now();

			channel.basicPublish(EXCHANGE_NAME, "", null, message.getBytes(StandardCharsets.UTF_8));
			System.out.println(" [x] Sent '" + message + "'");
		}
	}

}