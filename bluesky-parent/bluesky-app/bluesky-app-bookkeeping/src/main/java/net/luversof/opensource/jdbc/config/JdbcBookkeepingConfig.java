package net.luversof.opensource.jdbc.config;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "classpath:jdbc-bookkeeping.properties", ignoreResourceNotFound = true)
@PropertySource(value = "classpath:jdbc-bookkeeping-${spring.profiles.active}.properties", ignoreResourceNotFound = true)
public class JdbcBookkeepingConfig {
	
	@Bean
	@ConfigurationProperties("datasource.bookkeeping")
	public DataSourceProperties bookkeepingDataSourceProperties() {
		return new DataSourceProperties();
	}
	
	@Bean
	public DataSource bookkeepingDataSource() {
		return bookkeepingDataSourceProperties().initializeDataSourceBuilder().build();
	}
}

