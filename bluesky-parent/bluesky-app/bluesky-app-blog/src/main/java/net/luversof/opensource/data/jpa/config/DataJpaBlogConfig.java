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
@EnableJpaRepositories(basePackages = "net.luversof.blog", entityManagerFactoryRef = "blogEntityManagerFactory", transactionManagerRef = "blogTransactionManager")
public class DataJpaBlogConfig {
	
	@Bean(name = "blogEntityManagerFactory")
	public LocalContainerEntityManagerFactoryBean blogEntityManagerFactory(EntityManagerFactoryBuilder builder, @Qualifier("blogDataSource") DataSource blogDataSource) {
		return builder.dataSource(blogDataSource).packages("net.luversof.blog").build();
	}
	
	@Bean(name = "blogTransactionManager")
	public PlatformTransactionManager blogTransactionManager(@Qualifier("blogEntityManagerFactory") LocalContainerEntityManagerFactoryBean blogEntityManagerFactory) {
		JpaTransactionManager transactionManager = new JpaTransactionManager();
		transactionManager.setEntityManagerFactory(blogEntityManagerFactory.getObject());
		return transactionManager;
	}
}