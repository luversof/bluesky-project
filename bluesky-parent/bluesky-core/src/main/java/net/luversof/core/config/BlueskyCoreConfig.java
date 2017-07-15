package net.luversof.core.config;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.web.client.RestTemplate;

import net.luversof.core.util.CoreUtil;

@Configuration
@PropertySource("classpath:core.properties")
public class BlueskyCoreConfig {
	

	@Autowired
	private MessageSourceAccessor messageSourceAccessor;
	
	@PostConstruct
	public void postConstruct() {
		CoreUtil.setMessageSourceAccessor(messageSourceAccessor);
	}
	
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
	
	@Bean
	public MessageSourceAccessor messageSourceAccessor(MessageSource messageSource) {
		return new MessageSourceAccessor(messageSource);
	}
	
}
