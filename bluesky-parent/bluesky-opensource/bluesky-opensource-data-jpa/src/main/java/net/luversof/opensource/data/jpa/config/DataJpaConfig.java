package net.luversof.opensource.data.jpa.config;



import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@PropertySource("classpath:data-jpa.properties")
@PropertySource("classpath:data-jpa-${spring.profiles.active}.properties")
@EnableJpaAuditing
//@EntityScan(basePackages = "net.luversof")
public class DataJpaConfig {
	
//	@Bean
//	public LocalContainerEntityManagerFactoryBean entityManagerFactory(EntityManagerFactoryBuilder factoryBuilder, RoutingDataSource routingDataSource) {
////		Properties jpaProperties = new Properties();
////		jpaProperties.setProperty("hibernate.enable_lazy_load_no_trans", "true");
////		jpaProperties.setProperty("hibernate.auto_close_session", "true");
////		jpaProperties.setProperty("jadira.usertype.autoRegisterUserTypes", "true");
//		
//		Map<String, String> properties = new HashMap<>();
//		properties.put("jadira.usertype.autoRegisterUserTypes", "true");
//		return factoryBuilder.dataSource(routingDataSource).properties(properties).packages("net.luversof").build();
//	}
	
//	@Bean
//	public LocalContainerEntityManagerFactoryBean entityManagerFactory2(EntityManagerFactoryBuilder factoryBuilder, RoutingDataSource routingDataSource) {
////		Properties jpaProperties = new Properties();
////		jpaProperties.setProperty("hibernate.enable_lazy_load_no_trans", "true");
////		jpaProperties.setProperty("hibernate.auto_close_session", "true");
////		jpaProperties.setProperty("jadira.usertype.autoRegisterUserTypes", "true");
//		
//		Map<String, String> properties = new HashMap<>();
//		properties.put("jadira.usertype.autoRegisterUserTypes", "true");
//		return factoryBuilder.dataSource(routingDataSource).properties(properties).packages("net.luversof").build();
//	}
}