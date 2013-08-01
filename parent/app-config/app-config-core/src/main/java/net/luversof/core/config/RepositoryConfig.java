package net.luversof.core.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import lombok.SneakyThrows;
import net.luversof.core.datasource.DataSourceType;
import net.luversof.core.datasource.RoutingDataSource;

import org.apache.tomcat.dbcp.dbcp.BasicDataSource;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.hibernate4.HibernateExceptionTranslator;
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableAspectJAutoProxy
@EnableTransactionManagement
@EnableJpaRepositories(basePackages="net.luversof")
@ImportResource("classpath:net/luversof/core/config/repository/RepositoryContext.xml")
public class RepositoryConfig {

	@Value("${datasource.driverClassName}")
	private String driverClassName;

	@Value("${datasource.url}")
	private String url;

	@Value("${datasource.username}")
	private String username;

	@Value("${datasource.password}")
	private String password;

	@Value("${hibernate.packagesToScan[]}")
	private String[] packagesToScan;

	@Value("${hibernate.dialect}")
	private String dialect;

	@Value("${hibernate.show_sql}")
	private boolean showSql;

	@Value("${hibernate.format_sql}")
	private boolean formatSql;

	@Value("${hibernate.hbm2ddl.auto}")
	private String hbm2ddlAuto;

	@Value("${hibernate.hbm2ddl.import_files}")
	private String hbm2ddlImportFiles;

	@Bean(name = "testDataSource", destroyMethod = "close")
	public DataSource testDataSource() {
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setDriverClassName(driverClassName);
		dataSource.setUrl(url);
		dataSource.setUsername(username);
		dataSource.setPassword(password);
		return dataSource;
	}
	
	@Bean
	public RoutingDataSource dataSource() {
		RoutingDataSource routingDataSource = new RoutingDataSource();
		Map<Object, Object> targetDataSources = new HashMap<>();
		targetDataSources.put(DataSourceType.DEFAULT, testDataSource());
		targetDataSources.put(DataSourceType.TEST, testDataSource());
		routingDataSource.setTargetDataSources(targetDataSources);
		routingDataSource.setDefaultTargetDataSource(testDataSource());
		return routingDataSource;
	}
	
	@Bean
	@SneakyThrows
	public SessionFactory sessionFactory() {
		LocalSessionFactoryBean localSessionFactoryBean = new LocalSessionFactoryBean();
		localSessionFactoryBean.setDataSource(dataSource());
		localSessionFactoryBean.setPackagesToScan(packagesToScan);

		Properties hibernateProperties = new Properties();
		hibernateProperties.put("hibernate.dialect", dialect);
		hibernateProperties.put("hibernate.format_sql", formatSql);
		hibernateProperties.put("hibernate.show_sql", showSql);
		hibernateProperties.put("hibernate.hbm2ddl.auto", hbm2ddlAuto);
		hibernateProperties.put("hibernate.hbm2ddl.import_files", hbm2ddlImportFiles);
		localSessionFactoryBean.setHibernateProperties(hibernateProperties);

		localSessionFactoryBean.afterPropertiesSet();
		return localSessionFactoryBean.getObject();
	}

	@Bean
	public EntityManagerFactory entityManagerFactory() {
		HibernateJpaVendorAdapter jpaVendorAdapter = new HibernateJpaVendorAdapter();
		jpaVendorAdapter.setDatabase(Database.MYSQL);
		jpaVendorAdapter.setGenerateDdl(true);
		jpaVendorAdapter.setShowSql(true);

		LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
		entityManagerFactoryBean.setDataSource(dataSource());
		entityManagerFactoryBean.setJpaVendorAdapter(jpaVendorAdapter);
		entityManagerFactoryBean.setPackagesToScan("net.luversof");
		entityManagerFactoryBean.setMappingResources("META-INF/orm.xml");
		entityManagerFactoryBean.afterPropertiesSet();
		return entityManagerFactoryBean.getObject();
	}

	@Bean
	public PlatformTransactionManager transactionManager() {
		JpaTransactionManager txManager = new JpaTransactionManager();
		txManager.setEntityManagerFactory(entityManagerFactory());
		return txManager;
	}

	@Bean
	public HibernateExceptionTranslator hibernateExceptionTranslator() {
		return new HibernateExceptionTranslator();
	}
}