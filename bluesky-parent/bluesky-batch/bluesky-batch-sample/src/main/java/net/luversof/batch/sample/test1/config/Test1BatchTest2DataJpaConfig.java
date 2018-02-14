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
@EnableJpaRepositories(basePackages = "net.luversof.batch.**.repository.batchtest2", entityManagerFactoryRef = "batchTest2EntityManagerFactory", transactionManagerRef = "batchTest2TransactionManager")
public class Test1BatchTest2DataJpaConfig {


	@Bean
	public LocalContainerEntityManagerFactoryBean batchTest2EntityManagerFactory(EntityManagerFactoryBuilder builder, @Qualifier("batchTest2DataSource") DataSource batchTest1DataSource) {
		return builder
				.dataSource(batchTest1DataSource)
				.persistenceUnit("batchTest2PersistenceUnit")
				.packages("net.luversof.batch.**.domain.batchtest2").build();
	}
	
	@Bean
	public PlatformTransactionManager batchTest2TransactionManager(@Qualifier("batchTest2EntityManagerFactory") LocalContainerEntityManagerFactoryBean batchTest2EntityManagerFactory) {
		return new JpaTransactionManager(batchTest2EntityManagerFactory.getObject());
	}
}
