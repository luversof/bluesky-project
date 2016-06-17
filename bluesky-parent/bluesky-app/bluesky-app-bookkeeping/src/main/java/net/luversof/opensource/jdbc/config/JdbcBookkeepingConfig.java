package net.luversof.opensource.jdbc.config;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "classpath:jdbc-bookkeeping.properties", ignoreResourceNotFound = true)
@PropertySource(value = "classpath:jdbc-bookkeeping-${spring.profiles.active}.properties", ignoreResourceNotFound = true)
public class JdbcBookkeepingConfig {
	
	@Bean
	@ConfigurationProperties(prefix = "datasource.bookkeeping")
	public DataSource bookkeepingDataSource() {
		return DataSourceBuilder.create().build();
	}
}
