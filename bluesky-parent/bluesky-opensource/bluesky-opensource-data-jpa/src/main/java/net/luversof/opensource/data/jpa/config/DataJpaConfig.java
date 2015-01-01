package net.luversof.opensource.data.jpa.config;



import java.util.HashMap;
import java.util.Map;

import net.luversof.opensource.jdbc.routing.RoutingDataSource;

import org.springframework.boot.autoconfigure.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

@Configuration
@PropertySource("data-jpa.properties")
@PropertySource("data-jpa-${spring.profiles.active}.properties")
@EnableJpaAuditing
public class DataJpaConfig {
	
	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory(EntityManagerFactoryBuilder factoryBuilder, RoutingDataSource routingDataSource) {
//		Properties jpaProperties = new Properties();
//		jpaProperties.setProperty("hibernate.enable_lazy_load_no_trans", "true");
//		jpaProperties.setProperty("hibernate.auto_close_session", "true");
//		jpaProperties.setProperty("jadira.usertype.autoRegisterUserTypes", "true");
		
		Map<String, String> properties = new HashMap<>();
		properties.put("jadira.usertype.autoRegisterUserTypes", "true");
		return factoryBuilder.dataSource(routingDataSource).properties(properties).packages("net.luversof").build();
	}
}