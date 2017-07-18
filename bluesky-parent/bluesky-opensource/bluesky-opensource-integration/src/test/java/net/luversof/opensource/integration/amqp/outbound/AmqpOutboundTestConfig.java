package net.luversof.opensource.integration.amqp.outbound;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.messaging.MessageChannel;

@Configuration
@IntegrationComponentScan
public class AmqpOutboundTestConfig {

	@Bean
	public IntegrationFlow amqpOutbound(AmqpTemplate amqpTemplate) {
		return IntegrationFlows.from(amqpOutboundChannel()).handle(Amqp.outboundAdapter(amqpTemplate).routingKey("foo"))
				.get();
	}

	@Bean
	public MessageChannel amqpOutboundChannel() {
		return new DirectChannel();
	}

	@MessagingGateway(defaultRequestChannel = "amqpOutboundChannel")
	public interface MyGateway {
		void sendToRabbit(String data);
	}
}
