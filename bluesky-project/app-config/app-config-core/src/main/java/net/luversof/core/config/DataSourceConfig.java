package net.luversof.core.config;

import javax.sql.DataSource;

import org.apache.tomcat.dbcp.dbcp.BasicDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSourceConfig {
	
	@Value("${dataSource.default.driverClassName}")
	private String driverClassName;
	
	@Value("${dataSource.default.testOnBorrow}")
	private boolean testOnBorrow;
	
	@Value("${dataSource.default.testOnReturn}")
	private boolean testOnReturn;
	
	@Value("${dataSource.default.testWhileIdle}")
	private boolean testWhileIdle;
	
	@Value("${dataSource.default.validationQuery}")
	private String validationQuery;
	
	@Value("${dataSource.default.maxActive}")
	private int maxActive;
	
	@Value("${dataSource.default.maxIdle}")
	private int maxIdle;
	
	@Value("${dataSource.default.minIdle}")
	private int minIdle;
	
	
	private void setDataSourceDefaultConfig(BasicDataSource dataSource) {
		dataSource.setDriverClassName(driverClassName);
		dataSource.setTestOnBorrow(testOnBorrow);
		dataSource.setTestOnReturn(testOnReturn);
		dataSource.setTestWhileIdle(testWhileIdle);
		dataSource.setValidationQuery(validationQuery);
		dataSource.setMaxActive(maxActive);
		dataSource.setMaxIdle(maxIdle);
		dataSource.setMinIdle(minIdle);
	}
	

	@Bean(name = "defaultDataSource", destroyMethod = "close")
	public DataSource defaultDataSource(@Value("${dataSource.default.url}") String url, @Value("${dataSource.default.username}") String username, @Value("${dataSource.default.password}") String password) {
		BasicDataSource dataSource = new BasicDataSource();
		setDataSourceDefaultConfig(dataSource);
		dataSource.setUrl(url);
		dataSource.setUsername(username);
		dataSource.setPassword(password);
		return dataSource;
	}

	@Bean(name = "blogDataSource", destroyMethod = "close")
	public DataSource blogDataSource(@Value("${dataSource.blog.url}") String url, @Value("${dataSource.blog.username}") String username, @Value("${dataSource.blog.password}") String password) {
		BasicDataSource dataSource = new BasicDataSource();
		setDataSourceDefaultConfig(dataSource);
		dataSource.setUrl(url);
		dataSource.setUsername(username);
		dataSource.setPassword(password);
		return dataSource;
	}

	@Bean(name = "securityDataSource", destroyMethod = "close")
	public DataSource securityDataSource(@Value("${dataSource.security.url}") String url, @Value("${dataSource.security.username}") String username, @Value("${dataSource.security.password}") String password) {
		BasicDataSource dataSource = new BasicDataSource();
		setDataSourceDefaultConfig(dataSource);
		dataSource.setUrl(url);
		dataSource.setUsername(username);
		dataSource.setPassword(password);
		return dataSource;
	}
}
