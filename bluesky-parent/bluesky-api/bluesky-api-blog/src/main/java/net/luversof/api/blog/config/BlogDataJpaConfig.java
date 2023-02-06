package net.luversof.api.blog.config;



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
@EnableJpaRepositories(basePackages = "net.luversof.api.blog.repository.mariadb", entityManagerFactoryRef = "blogEntityManagerFactory", transactionManagerRef = "blogTransactionManager")
public class BlogDataJpaConfig {
	
	@Bean(name = "blogEntityManagerFactory")
	public LocalContainerEntityManagerFactoryBean blogEntityManagerFactory(EntityManagerFactoryBuilder builder, @Qualifier("blogDataSource") DataSource blogDataSource) {
		return builder
				.dataSource(blogDataSource)
				.persistenceUnit("blogPersistenceUnit")
				.packages("net.luversof.api.blog.domain.mariadb").build();
	}
	
	@Bean(name = "blogTransactionManager")
	public PlatformTransactionManager blogTransactionManager(@Qualifier("blogEntityManagerFactory") LocalContainerEntityManagerFactoryBean blogEntityManagerFactory) {
		return new JpaTransactionManager(blogEntityManagerFactory.getObject());
	}
}