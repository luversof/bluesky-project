package net.luversof.batch.sample.test1.config;

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
@EnableJpaRepositories(basePackages = "net.luversof.batch.**.repository.batchtest1", entityManagerFactoryRef = "batchTest1EntityManagerFactory", transactionManagerRef = "batchTest1TransactionManager")
public class Test1BatchTest1DataJpaConfig {


	@Bean
	public LocalContainerEntityManagerFactoryBean batchTest1EntityManagerFactory(EntityManagerFactoryBuilder builder, @Qualifier("batchTest1DataSource") DataSource batchTest1DataSource) {
		return builder
				.dataSource(batchTest1DataSource)
				.persistenceUnit("batchTest1PersistenceUnit")
				.packages("net.luversof.batch.**.domain.batchtest1").build();
	}
	
	@Bean
	public PlatformTransactionManager batchTest1TransactionManager(@Qualifier("batchTest1EntityManagerFactory") LocalContainerEntityManagerFactoryBean batchTest1EntityManagerFactory) {
		return new JpaTransactionManager(batchTest1EntityManagerFactory.getObject());
	}
}
