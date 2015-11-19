package net.luversof.opensource.data.jpa.config;



import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableJpaRepositories(basePackages = "net.luversof.user", entityManagerFactoryRef = "securityEntityManagerFactory", transactionManagerRef = "securityTransactionManager")
public class UserDataJpaConfig {
	
	@Bean(name = "securityEntityManagerFactory")
	public LocalContainerEntityManagerFactoryBean securityEntityManagerFactory(EntityManagerFactoryBuilder builder, @Qualifier("securityDataSource") DataSource securityDataSource) {
		return builder.dataSource(securityDataSource).packages("net.luversof.user").build();
	}
	
	@Bean(name = "securityTransactionManager")
	public PlatformTransactionManager securityTransactionManager(@Qualifier("securityEntityManagerFactory") LocalContainerEntityManagerFactoryBean securityEntityManagerFactory) {
		JpaTransactionManager transactionManager = new JpaTransactionManager();
		transactionManager.setEntityManagerFactory(securityEntityManagerFactory.getObject());
		return transactionManager;
	}
}