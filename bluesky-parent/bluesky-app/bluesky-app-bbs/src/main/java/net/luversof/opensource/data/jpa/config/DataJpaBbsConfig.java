package net.luversof.opensource.data.jpa.config;



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
@EnableJpaRepositories(basePackages = "net.luversof.bbs", entityManagerFactoryRef = "bbsEntityManagerFactory", transactionManagerRef = "bbsTransactionManager")
public class DataJpaBbsConfig {
	
	@Bean
	public LocalContainerEntityManagerFactoryBean bbsEntityManagerFactory(EntityManagerFactoryBuilder builder, @Qualifier("bbsDataSource") DataSource bbsDataSource) {
		return builder.dataSource(bbsDataSource).persistenceUnit("bbsPersistenceUnit").packages(Jsr310JpaConverters.class).packages("net.luversof.bbs").build();
	}
	
	@Bean
	public PlatformTransactionManager bbsTransactionManager(@Qualifier("bbsEntityManagerFactory") LocalContainerEntityManagerFactoryBean bbsEntityManagerFactory) {
		JpaTransactionManager transactionManager = new JpaTransactionManager();
		transactionManager.setEntityManagerFactory(bbsEntityManagerFactory.getObject());
		return transactionManager;
	}
}