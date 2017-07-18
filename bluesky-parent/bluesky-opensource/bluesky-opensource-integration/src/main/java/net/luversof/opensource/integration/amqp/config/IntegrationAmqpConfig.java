package net.luversof.opensource.integration.amqp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:integration-amqp-${spring.profiles.active}.properties")
public class IntegrationAmqpConfig {

	
}
