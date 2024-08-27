package net.luversof.api.bookkeeping.config;

import java.util.HashMap;
import java.util.Properties;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BookkeepingDataJpaConfig {

	@Configuration
	@EnableJpaRepositories(basePackages = "net.luversof.api.bookkeeping.**.repository.mariadb", entityManagerFactoryRef = "bookkeepingEntityManagerFactory", transactionManagerRef = "bookkeepingTransactionManager")
	public class BookkeepingMariadbDataJpaConfig {

	    @Bean
	    LocalContainerEntityManagerFactoryBean bookkeepingEntityManagerFactory(EntityManagerFactoryBuilder builder, @Qualifier("bookkeepingDataSource") DataSource dataSource) {
	        return builder
	                .dataSource(dataSource)
	                .persistenceUnit("bookkeepingPersistenceUnit")
	                .packages(Jsr310JpaConverters.class)
	                .packages("net.luversof.api.bookkeeping.**.domain").build();
	    }

	    @Bean
	    PlatformTransactionManager bookkeepingTransactionManager(@Qualifier("bookkeepingEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
	        return new JpaTransactionManager(entityManagerFactory.getObject());
	    }
	}
	
	@Configuration
	@EnableJpaRepositories(basePackages = "net.luversof.api.bookkeeping.**.repository.postgresql", entityManagerFactoryRef = "bookkeepingPostgresqlEntityManagerFactory", transactionManagerRef = "bookkeepingPostgresqlTransactionManager")
	public class BookkeepingPostgresqlDataJpaConfig {
		
		@Bean
	    LocalContainerEntityManagerFactoryBean bookkeepingPostgresqlEntityManagerFactory(EntityManagerFactoryBuilder builder, @Qualifier("bookkeepingPostgresqlDataSource") DataSource dataSource) {
			
			var hibernateProperties = new HashMap<String, String>();
//			hibernateProperties.put("hibernate.jdbc.lob.non_contextual_creation", "true");
//			hibernateProperties.put("hibernate.temp.use_jdbc_metadata_defaults", "false");
			hibernateProperties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
			
	        return builder
	                .dataSource(dataSource)
	                .persistenceUnit("bookkeepingPostgresqlPersistenceUnit")
	                .packages(Jsr310JpaConverters.class)
	                .properties(hibernateProperties)
	                .packages("net.luversof.api.bookkeeping.**.domain").build();
	    }
	    
	    @Bean
	    PlatformTransactionManager bookkeepingPostgresqlTransactionManager(@Qualifier("bookkeepingPostgresqlEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
	        return new JpaTransactionManager(entityManagerFactory.getObject());
	    }

	}
}
