package net.luversof.opensource.data.jpa.config;



import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableJpaRepositories(basePackages = "net.luversof.bookkeeping", entityManagerFactoryRef = "bookkeepingEntityManagerFactory", transactionManagerRef = "bookkeepingTransactionManager")
public class BookkeepingDataJpaConfig {
	
	@Bean(name = "bookkeepingEntityManagerFactory")
	public LocalContainerEntityManagerFactoryBean bookkeepingEntityManagerFactory(EntityManagerFactoryBuilder builder, @Qualifier("bookkeepingDataSource") DataSource bookkeepingDataSource) {
		return builder.dataSource(bookkeepingDataSource).packages("net.luversof.bookkeeping").build();
	}
	
	@Bean(name = "bookkeepingTransactionManager")
	public PlatformTransactionManager bookkeepingTransactionManager(@Qualifier("bookkeepingEntityManagerFactory") LocalContainerEntityManagerFactoryBean bookkeepingEntityManagerFactory) {
		JpaTransactionManager transactionManager = new JpaTransactionManager();
		transactionManager.setEntityManagerFactory(bookkeepingEntityManagerFactory.getObject());
		return transactionManager;
	}
}