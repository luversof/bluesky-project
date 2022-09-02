package net.luversof.core.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;

import net.luversof.core.service.UserIdService;
import net.luversof.core.service.NullUserIdService;


@Configuration
@ComponentScan("net.luversof.core")
@PropertySource("classpath:core.properties")
public class BlueskyCoreConfig {
	
	@Primary
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
	
	@LoadBalanced
	@Bean
	public RestTemplate loadBalancedRestTemplate() {
		return new RestTemplate();
	}
	
	@Bean
	public Hibernate5Module hibernate5Module(){
		return new Hibernate5Module();
	}
	
	@ConditionalOnMissingBean
	@Bean
	public UserIdService userIdService() {
		return new NullUserIdService();
	}
	
}
