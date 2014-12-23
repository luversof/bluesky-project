package net.luversof.opensource.data.jpa.config;



import net.luversof.opensource.jdbc.routing.RoutingDataSource;

import org.springframework.boot.autoconfigure.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

@Configuration
@PropertySource("data-jpa-${spring.profiles.active}.properties")
public class DataJpaConfig {
	
//	@Autowired
//	private RoutingDataSource routingDataSource;
	
	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory(EntityManagerFactoryBuilder factoryBuilder, RoutingDataSource routingDataSource) {
		return factoryBuilder.dataSource(routingDataSource).packages("net.luversof").build();
	}
}