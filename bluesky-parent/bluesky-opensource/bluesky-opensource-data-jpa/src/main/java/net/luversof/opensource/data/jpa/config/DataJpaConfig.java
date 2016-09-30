package net.luversof.opensource.data.jpa.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitManager;

@Configuration
@EnableJpaAuditing
@PropertySource("classpath:data-jpa.properties")
@PropertySource("classpath:data-jpa-${spring.profiles.active}.properties")
public class DataJpaConfig {

	/**
	 * spring-boot 1.4.1-RELEASE 버그로인해 선언. 1.4.2 이후 제거해야함
	 * @param properties
	 * @param jpaVendorAdapter
	 * @param persistenceUnitManagerProvider
	 * @return
	 */
	@Bean
	public EntityManagerFactoryBuilder entityManagerFactoryBuilder(JpaProperties properties, JpaVendorAdapter jpaVendorAdapter, ObjectProvider<PersistenceUnitManager> persistenceUnitManagerProvider) {
		return new EntityManagerFactoryBuilder(jpaVendorAdapter, properties.getProperties(), persistenceUnitManagerProvider.getIfAvailable(), null);
	}

}
