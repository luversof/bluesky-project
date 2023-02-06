package net.luversof.api.board.config;



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
@EnableJpaRepositories(basePackages = "net.luversof.api.board.**.repository", entityManagerFactoryRef = "boardEntityManagerFactory", transactionManagerRef = "boardTransactionManager")
public class BoardDataJpaonfig {
	
	@Bean
	public LocalContainerEntityManagerFactoryBean boardEntityManagerFactory(EntityManagerFactoryBuilder builder, @Qualifier("boardDataSource") DataSource boardDataSource) {
		return builder
				.dataSource(boardDataSource)
				.persistenceUnit("boardPersistenceUnit")
				.packages("net.luversof.api.board.**.domain").build();
	}
	
	@Bean
	public PlatformTransactionManager boardTransactionManager(@Qualifier("boardEntityManagerFactory") LocalContainerEntityManagerFactoryBean boardEntityManagerFactory) {
		return new JpaTransactionManager(boardEntityManagerFactory.getObject());
	}
}