package net.luversof.data.jpa.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Resource;
import javax.sql.DataSource;

import net.luversof.data.jpa.datasource.DataSourceType;
import net.luversof.data.jpa.datasource.RoutingDataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.hibernate4.HibernateExceptionTranslator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages="net.luversof")
@ImportResource("classpath:net/luversof/data/jpa/config/repository/RepositoryContext.xml")
public class RepositoryConfig {

	@Value("${jpaVendorAdapter.showSql}")
	private boolean showSql;
	
	@Value("${jpaVendorAdapter.generateDdl}")
	private boolean generateDdl;
	
	@Value("${jpaVendorAdapter.databasePlatform}")
	private String databasePlatform;
	
	@Value("${entityManagerFactoryBean.packagesToScan}")
	private String packagesToScan;
	
	@Value("${entityManagerFactoryBean.mappingResources}")
	private String mappingResources;
	
	@Resource(name = "defaultDataSource")
	private DataSource defaultDataSource;
	
	@Resource(name = "securityDataSource")
	private DataSource securityDataSource;
	
	@Resource(name = "blogDataSource")
	private DataSource blogDataSource;
	
	@Resource(name = "bookkeepingDataSource")
	private DataSource bookkeepingDataSource;
	
	@Bean
	public RoutingDataSource dataSource() {
		RoutingDataSource routingDataSource = new RoutingDataSource();
		Map<Object, Object> targetDataSources = new HashMap<>();
		targetDataSources.put(DataSourceType.DEFAULT, defaultDataSource);
		targetDataSources.put(DataSourceType.SECURITY, securityDataSource);
		targetDataSources.put(DataSourceType.BLOG, blogDataSource);
		targetDataSources.put(DataSourceType.BOOKKEEPING, bookkeepingDataSource);
		routingDataSource.setTargetDataSources(targetDataSources);
		routingDataSource.setDefaultTargetDataSource(defaultDataSource);
		return routingDataSource;
	}

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

		LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
		entityManagerFactoryBean.setDataSource(dataSource());
		entityManagerFactoryBean.setJpaVendorAdapter(jpaVendorAdapter);
		entityManagerFactoryBean.setJpaProperties(jpaProperties);
		entityManagerFactoryBean.setPackagesToScan(packagesToScan);
		entityManagerFactoryBean.setMappingResources(mappingResources);
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