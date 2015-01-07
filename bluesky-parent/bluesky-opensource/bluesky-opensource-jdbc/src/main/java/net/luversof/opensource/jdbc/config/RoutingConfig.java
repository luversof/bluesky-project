package net.luversof.opensource.jdbc.config;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import net.luversof.opensource.jdbc.routing.DataSourceType;
import net.luversof.opensource.jdbc.routing.RoutingDataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class RoutingConfig {
	
	@Autowired
	private DataSource memberDataSource;
	
	@Autowired
	private DataSource blogDataSource;
	
	@Autowired
	private DataSource bookkeepingDataSource;
	
	@Bean
	@Primary
	public RoutingDataSource routingDataSource() {
		RoutingDataSource routingDataSource = new RoutingDataSource();
		Map<Object, Object> targetDataSources = new HashMap<>();
		targetDataSources.put(DataSourceType.MEMBER, memberDataSource);
		targetDataSources.put(DataSourceType.BLOG, blogDataSource);
		targetDataSources.put(DataSourceType.BOOKKEEPING, bookkeepingDataSource);
		routingDataSource.setTargetDataSources(targetDataSources);
		routingDataSource.setDefaultTargetDataSource(blogDataSource);
		return routingDataSource;
	}
}
