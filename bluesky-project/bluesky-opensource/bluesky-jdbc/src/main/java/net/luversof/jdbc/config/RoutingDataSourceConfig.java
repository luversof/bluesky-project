package net.luversof.jdbc.config;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.sql.DataSource;

import net.luversof.jdbc.datasource.DataSourceType;
import net.luversof.jdbc.datasource.RoutingDataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RoutingDataSourceConfig {
	
	@Resource
	private DataSource defaultDataSource;

	@Resource
	private DataSource securityDataSource;

	@Resource
	private DataSource blogDataSource;

	@Resource
	private DataSource bookkeepingDataSource;
	
	@Bean
	public RoutingDataSource routingDataSource() {
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
}
