package net.luversof.opensource.integration.amqp.outbound;

import java.util.Date;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.Payload;


@Configuration
@RabbitListener(queues = "foo2")
public class SimpleTestConfig {

	@Bean
	public Queue fooQueue() {
		return new Queue("foo2");
	}

	@RabbitHandler
	public void process(@Payload String foo2) {
		System.out.println(new Date() + ": " + foo2);
	}

}
