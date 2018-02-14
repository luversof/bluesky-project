package net.luversof.batch.sample.config;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "classpath:batch-jdbc.properties", ignoreResourceNotFound = true)
@PropertySource(value = "classpath:batch-jdbc-${spring.profiles.active}.properties", ignoreResourceNotFound = true)
public class BatchJdbcConfig {

	@Bean
	@ConfigurationProperties("datasource.batch")
	public DataSourceProperties batchDataSourceProperties() {
		return new DataSourceProperties();
	}
	
	@Bean
	public DataSource batchDataSource() {
		return batchDataSourceProperties().initializeDataSourceBuilder().build();
	}
}
