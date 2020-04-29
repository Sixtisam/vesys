package ch.fhnw.ds.rabbitmq.echo;

import java.nio.charset.StandardCharsets;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class Server {

	static final String RPC_QUEUE_NAME = "echo";

	public static void main(String[] argv) throws Exception {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setUsername("vesys-dev");
		factory.setPassword("vesys123");
		factory.setVirtualHost("vesys-dev-vhost");
		factory.setHost("86.119.38.130");
		factory.setPort(5672);

		Connection connection = factory.newConnection();
		Channel channel = connection.createChannel();

		channel.queueDeclare(RPC_QUEUE_NAME, false, false, false, null);

//		channel.basicQos(1);
		boolean autoAck = true;

		System.out.println(" [x] Awaiting RPC requests");

		DeliverCallback deliverCallback = (consumerTag, delivery) -> {
			AMQP.BasicProperties replyProps = new AMQP.BasicProperties.Builder()
					.correlationId(delivery.getProperties().getCorrelationId()).build();

			String response = "";

			try {
				String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
				response = "Echo: " + message;
				System.out.println(" [.] " + response);
			} finally {
				channel.basicPublish("", delivery.getProperties().getReplyTo(), replyProps, response.getBytes("UTF-8"));
//				channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
			}
		};

		channel.basicConsume(RPC_QUEUE_NAME, autoAck, deliverCallback, (consumerTag -> {
		}));

	}
}