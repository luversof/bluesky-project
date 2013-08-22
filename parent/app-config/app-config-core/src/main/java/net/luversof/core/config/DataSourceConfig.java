package net.luversof.core.config;

import javax.sql.DataSource;

import org.apache.tomcat.dbcp.dbcp.BasicDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSourceConfig {

	@Bean(name = "defaultDataSource", destroyMethod = "close")
	public DataSource defaultDataSource(@Value("${datasource.default.driverClassName}") String driverClassName, @Value("${datasource.default.url}") String url, @Value("${datasource.default.username}") String username, @Value("${datasource.default.password}") String password) {
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setDriverClassName(driverClassName);
		dataSource.setUrl(url);
		dataSource.setUsername(username);
		dataSource.setPassword(password);
		return dataSource;
	}

	@Bean(name = "blogDataSource", destroyMethod = "close")
	public DataSource blogDataSource(@Value("${datasource.blog.driverClassName}") String driverClassName, @Value("${datasource.blog.url}") String url, @Value("${datasource.blog.username}") String username, @Value("${datasource.blog.password}") String password) {
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setDriverClassName(driverClassName);
		dataSource.setUrl(url);
		dataSource.setUsername(username);
		dataSource.setPassword(password);
		return dataSource;
	}
	
	@Bean(name = "securityDataSource", destroyMethod = "close")
	public DataSource securityDataSource(@Value("${datasource.security.driverClassName}") String driverClassName, @Value("${datasource.security.url}") String url, @Value("${datasource.security.username}") String username, @Value("${datasource.security.password}") String password) {
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setDriverClassName(driverClassName);
		dataSource.setUrl(url);
		dataSource.setUsername(username);
		dataSource.setPassword(password);
		return dataSource;
	}
}
