package net.luversof.core.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Resource;
import javax.sql.DataSource;

import net.luversof.core.datasource.DataSourceType;
import net.luversof.core.datasource.RoutingDataSource;

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
//@EnableAspectJAutoProxy
@EnableTransactionManagement
@EnableJpaRepositories(basePackages="net.luversof")
@ImportResource("classpath:net/luversof/core/config/repository/RepositoryContext.xml")
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
	
	@Resource(name = "blogDataSource")
	private DataSource blogDataSource;
	
	@Resource(name = "securityDataSource")
	private DataSource securityDataSource;
	
	@Bean
	public RoutingDataSource dataSource() {
		RoutingDataSource routingDataSource = new RoutingDataSource();
		Map<Object, Object> targetDataSources = new HashMap<>();
		targetDataSources.put(DataSourceType.DEFAULT, defaultDataSource);
		targetDataSources.put(DataSourceType.BLOG, blogDataSource);
		targetDataSources.put(DataSourceType.SECURITY, securityDataSource);
		routingDataSource.setTargetDataSources(targetDataSources);
		routingDataSource.setDefaultTargetDataSource(defaultDataSource);
		return routingDataSource;
	}
	
//	@Bean
//	@SneakyThrows
//	public SessionFactory sessionFactory() {
//		LocalSessionFactoryBean localSessionFactoryBean = new LocalSessionFactoryBean();
//		localSessionFactoryBean.setDataSource(dataSource());
//		localSessionFactoryBean.setPackagesToScan(packagesToScan);
//
//		Properties hibernateProperties = new Properties();
//		hibernateProperties.put("hibernate.dialect", dialect);
//		hibernateProperties.put("hibernate.format_sql", formatSql);
//		hibernateProperties.put("hibernate.show_sql", showSql);
//		hibernateProperties.put("hibernate.hbm2ddl.auto", hbm2ddlAuto);
//		hibernateProperties.put("hibernate.hbm2ddl.import_files", hbm2ddlImportFiles);
//		localSessionFactoryBean.setHibernateProperties(hibernateProperties);
//
//		localSessionFactoryBean.afterPropertiesSet();
//		return localSessionFactoryBean.getObject();
//	}

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
	
//	@Bean
//	public QueryDslJdbcTemplate queryDslJdbcTemplate() {
//		return new QueryDslJdbcTemplate(dataSource());
//	}

	@Bean
	public HibernateExceptionTranslator hibernateExceptionTranslator() {
		return new HibernateExceptionTranslator();
	}
}