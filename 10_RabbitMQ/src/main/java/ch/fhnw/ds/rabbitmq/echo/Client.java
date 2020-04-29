package ch.fhnw.ds.rabbitmq.echo;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class Client {

	private static final String RPC_QUEUE_NAME = Server.RPC_QUEUE_NAME;

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername("vesys-dev");			// Default: guest (only works on localhost)
        factory.setPassword("vesys123");			// Default: guest (only works on localhost)
        factory.setVirtualHost("vesys-dev-vhost");	// Default: /
		factory.setHost("86.119.38.130");			// Default: localhost
		factory.setPort(5672);						// Default: 5672
		
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
        	
			channel.queueDeclare(RPC_QUEUE_NAME, 
					/* durable:    */ false, 
					/* exclusive:  */ false,
					/* autoDelete: */ false,
					/* arguments:  */ null);
			
	        final String corrId = UUID.randomUUID().toString();

	        String replyQueueName = channel.queueDeclare().getQueue();
	        System.out.println(replyQueueName);
	        AMQP.BasicProperties props = new AMQP.BasicProperties
	                .Builder()
	                .correlationId(corrId)
	                .replyTo(replyQueueName)
	                .build();

	        String message = "Hello World at " + LocalDateTime.now();
            System.out.println(message);
            
            channel.basicPublish(
            		/* exchange:    */ "",			// Exchange: empty string is called "default exchang" which is a direct exchange. 
            		/* routing key: */ RPC_QUEUE_NAME,
            		/* props:       */ props,
            		/* body:        */ message.getBytes(StandardCharsets.UTF_8));


	        final BlockingQueue<String> response = new ArrayBlockingQueue<>(1);

	        String ctag = channel.basicConsume(replyQueueName, true, (consumerTag, delivery) -> {
	            if (delivery.getProperties().getCorrelationId().equals(corrId)) {
	                response.offer(new String(delivery.getBody(), "UTF-8"));
	            }
	        }, consumerTag -> {
	        });

	        String result = response.take();
	        channel.basicCancel(ctag);
	        
	        System.out.println(result);
        }
    }

}