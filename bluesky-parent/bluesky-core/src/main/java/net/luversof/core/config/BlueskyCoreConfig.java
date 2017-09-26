package net.luversof.core.config;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.validation.Validator;
import org.springframework.web.client.RestTemplate;

import net.luversof.core.util.ValidationUtil;

@Configuration
@ComponentScan("net.luversof.core")
@PropertySource("classpath:core.properties")
public class BlueskyCoreConfig {
	
	@Autowired
	private Validator validator;
	
	@PostConstruct
	public void postConstruct() {
		ValidationUtil.setValidator(validator);
	}
	
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
}
