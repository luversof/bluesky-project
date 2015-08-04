package net.luversof.opensource.jdbc.config;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("jdbc-${spring.profiles.active}.properties")
public class JdbcConfig {
	
//	@ConfigurationProperties 암복호화 처리 문제로 인해 아래 방식은 쓰지 못함
	
	@Bean
	@Primary
	@ConfigurationProperties(prefix = "datasource.security")
	public DataSource securityDataSource() {
		return DataSourceBuilder.create().build();
	}
	
	@Bean
	@ConfigurationProperties(prefix = "datasource.blog")
	public DataSource blogDataSource() {
		return DataSourceBuilder.create().build();
	}

	@Bean
	@ConfigurationProperties(prefix = "datasource.bookkeeping")
	public DataSource bookkeepingDataSource() {
		return DataSourceBuilder.create().build();
	}
	
//	@Bean
//	public DataSource securityDataSource(
//			@Value("${datasource.security.driverClassName}") String driverClassName,
//			@Value("${datasource.security.url}") String url,
//			@Value("${datasource.security.username}") String username,
//			@Value("${datasource.security.password}") String password
//			) {
//		DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
//		dataSourceBuilder.driverClassName(driverClassName);
//		dataSourceBuilder.url(url);
//		dataSourceBuilder.username(username);
//		dataSourceBuilder.password(password);
//		return dataSourceBuilder.build();
//	}
//	
//	@Bean
//	public DataSource blogDataSource(
//			@Value("${datasource.blog.driverClassName}") String driverClassName,
//			@Value("${datasource.blog.url}") String url,
//			@Value("${datasource.blog.username}") String username,
//			@Value("${datasource.blog.password}") String password
//			) {
//		DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
//		dataSourceBuilder.driverClassName(driverClassName);
//		dataSourceBuilder.url(url);
//		dataSourceBuilder.username(username);
//		dataSourceBuilder.password(password);
//		return dataSourceBuilder.build();
//	}
//	
//	@Bean
//	public DataSource bookkeepingDataSource(
//			@Value("${datasource.bookkeeping.driverClassName}") String driverClassName,
//			@Value("${datasource.bookkeeping.url}") String url,
//			@Value("${datasource.bookkeeping.username}") String username,
//			@Value("${datasource.bookkeeping.password}") String password
//			) {
//		DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
//		dataSourceBuilder.driverClassName(driverClassName);
//		dataSourceBuilder.url(url);
//		dataSourceBuilder.username(username);
//		dataSourceBuilder.password(password);
//		return dataSourceBuilder.build();
//	}
//	
//	@Bean
//	@Primary
//	public RoutingDataSource routingDataSource(
//			DataSource securityDataSource,
//			DataSource blogDataSource,
//			DataSource bookkeepingDataSource
//			) {
//		RoutingDataSource routingDataSource = new RoutingDataSource();
//		Map<Object, Object> targetDataSources = new HashMap<>();
//		targetDataSources.put(DataSourceType.MEMBER, securityDataSource);
//		targetDataSources.put(DataSourceType.BLOG, blogDataSource);
//		targetDataSources.put(DataSourceType.BOOKKEEPING, bookkeepingDataSource);
//		routingDataSource.setTargetDataSources(targetDataSources);
//		routingDataSource.setDefaultTargetDataSource(blogDataSource);
//		return routingDataSource;
//	}
}
