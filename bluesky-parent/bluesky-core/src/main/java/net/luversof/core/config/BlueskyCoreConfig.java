package net.luversof.core.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.web.client.RestTemplate;

import net.luversof.core.util.CoreUtil;

@Configuration
@ComponentScan("net.luversof.core")
@PropertySource("classpath:core.properties")
public class BlueskyCoreConfig {
	
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
	
	@Bean
	public MessageSourceAccessor messageSourceAccessor(MessageSource messageSource) {
		MessageSourceAccessor messageSourceAccessor = new MessageSourceAccessor(messageSource);
		CoreUtil.setMessageSourceAccessor(messageSourceAccessor);
		return messageSourceAccessor;
	}
	
}
