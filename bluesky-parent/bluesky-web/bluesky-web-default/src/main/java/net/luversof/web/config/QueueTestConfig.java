//package net.luversof.web.config;
//
//import org.springframework.amqp.core.AmqpTemplate;
//import org.springframework.amqp.rabbit.connection.ConnectionFactory;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.integration.amqp.dsl.Amqp;
//import org.springframework.integration.annotation.MessagingGateway;
//import org.springframework.integration.channel.DirectChannel;
//import org.springframework.integration.dsl.IntegrationFlow;
//import org.springframework.integration.dsl.IntegrationFlows;
//import org.springframework.messaging.MessageChannel;
//import org.springframework.stereotype.Service;
//
//@Configuration
//public class QueueTestConfig {
//
//	@Bean
//	public IntegrationFlow amqpOutbound(AmqpTemplate amqpTemplate) {
//		return IntegrationFlows.from(amqpOutboundChannel()).handle(Amqp.outboundAdapter(amqpTemplate).routingKey("foo"))
//				.get();
//	}
//
//	@Bean
//	public MessageChannel amqpOutboundChannel() {
//		return new DirectChannel();
//	}
//
//	@MessagingGateway(defaultRequestChannel = "amqpOutboundChannel")
//	public interface TestGateway {
//		void sendToRabbit(String data);
//	}
//	
//	@Bean
//	public IntegrationFlow amqpInbound(ConnectionFactory connectionFactory) {
//		return IntegrationFlows.from(Amqp.inboundAdapter(connectionFactory, "foo"))
//				.handle(m -> System.out.println(m.getPayload())).get();
//	}
//}
