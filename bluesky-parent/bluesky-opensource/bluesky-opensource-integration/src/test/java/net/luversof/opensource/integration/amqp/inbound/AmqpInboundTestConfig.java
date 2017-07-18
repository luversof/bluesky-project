package net.luversof.opensource.integration.amqp.inbound;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;

@Configuration
public class AmqpInboundTestConfig {

	@Bean
	public IntegrationFlow amqpInbound(ConnectionFactory connectionFactory) {
		return IntegrationFlows.from(Amqp.inboundAdapter(connectionFactory, "foo"))
				.handle(m -> System.out.println(m.getPayload())).get();
	}

}
