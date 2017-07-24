package net.luversof.opensource.data.jpa.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableJpaRepositories(basePackages = "net.luversof.user.**.repository", entityManagerFactoryRef = "userEntityManagerFactory", transactionManagerRef = "userTransactionManager")
public class DataJpaUserConfig {
	
	@Bean(name = "userEntityManagerFactory")
	@Primary
	public LocalContainerEntityManagerFactoryBean userEntityManagerFactory(EntityManagerFactoryBuilder builder, @Qualifier("userDataSource") DataSource userDataSource) {
		return builder.dataSource(userDataSource).persistenceUnit("userPersistenceUnit").packages("net.luversof.user.**.domain").build();
	}
	
	@Bean(name = "userTransactionManager")
	public PlatformTransactionManager userTransactionManager(@Qualifier("userEntityManagerFactory") LocalContainerEntityManagerFactoryBean securityEntityManagerFactory) {
		JpaTransactionManager transactionManager = new JpaTransactionManager();
		transactionManager.setEntityManagerFactory(securityEntityManagerFactory.getObject());
		return transactionManager;
	}
}