package net.luversof.data.jpa.config;

import java.util.Properties;

import net.luversof.jdbc.datasource.RoutingDataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.hibernate4.HibernateExceptionTranslator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class RepositoryConfig {

	@Value("${jpaVendorAdapter.showSql}")
	private boolean showSql;

	@Value("${jpaVendorAdapter.generateDdl}")
	private boolean generateDdl;

	@Value("${jpaVendorAdapter.databasePlatform}")
	private String databasePlatform;

	@Value("${entityManagerFactoryBean.packagesToScan}")
	private String packagesToScan;
	
	@Autowired
	private RoutingDataSource routingDataSource;

	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
		HibernateJpaVendorAdapter jpaVendorAdapter = new HibernateJpaVendorAdapter();
		jpaVendorAdapter.setDatabase(Database.MYSQL);
		jpaVendorAdapter.setGenerateDdl(generateDdl);
		jpaVendorAdapter.setShowSql(showSql);
		jpaVendorAdapter.setDatabasePlatform(databasePlatform);

		Properties jpaProperties = new Properties();
		jpaProperties.setProperty("hibernate.enable_lazy_load_no_trans", "true");
		jpaProperties.setProperty("hibernate.auto_close_session", "true");
		jpaProperties.setProperty("jadira.usertype.autoRegisterUserTypes", "true");

		LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
		entityManagerFactoryBean.setDataSource(routingDataSource);
		entityManagerFactoryBean.setJpaVendorAdapter(jpaVendorAdapter);
		entityManagerFactoryBean.setJpaProperties(jpaProperties);
		entityManagerFactoryBean.setPackagesToScan(packagesToScan);
		entityManagerFactoryBean.afterPropertiesSet();
		return entityManagerFactoryBean;
	}

	@Bean
	public PlatformTransactionManager transactionManager() {
		JpaTransactionManager txManager = new JpaTransactionManager();
		txManager.setEntityManagerFactory(entityManagerFactory().getObject());
		return txManager;
	}

	@Bean
	public HibernateExceptionTranslator hibernateExceptionTranslator() {
		return new HibernateExceptionTranslator();
	}
}
