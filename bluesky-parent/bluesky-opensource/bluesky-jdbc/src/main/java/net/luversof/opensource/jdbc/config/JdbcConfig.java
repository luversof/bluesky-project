package net.luversof.opensource.jdbc.config;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import net.luversof.opensource.jdbc.routing.DataSourceType;
import net.luversof.opensource.jdbc.routing.RoutingDataSource;

import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;

@Configuration
//@PropertySource("jdbc.properties")
@PropertySource("jdbc-${spring.profiles.active}.properties")
public class JdbcConfig {
	
	@Bean
	@Primary
	@ConfigurationProperties(prefix = "datasource.member")
	public DataSource memberDataSource() {
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
	
	@Bean
	public RoutingDataSource routingDataSource() {
		RoutingDataSource routingDataSource = new RoutingDataSource();
		Map<Object, Object> targetDataSources = new HashMap<>();
		targetDataSources.put(DataSourceType.MEMBER, memberDataSource());
		targetDataSources.put(DataSourceType.BLOG, blogDataSource());
		targetDataSources.put(DataSourceType.BOOKKEEPING, bookkeepingDataSource());
		routingDataSource.setTargetDataSources(targetDataSources);
		routingDataSource.setDefaultTargetDataSource(blogDataSource());
		return routingDataSource;
	}
}
