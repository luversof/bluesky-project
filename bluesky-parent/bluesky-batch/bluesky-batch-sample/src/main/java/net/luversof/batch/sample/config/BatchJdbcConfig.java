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
	@ConfigurationProperties("datasource.bbs")
	public DataSourceProperties bbsDataSourceProperties() {
		return new DataSourceProperties();
	}
	
	@Bean
	public DataSource bbsDataSource(DataSourceProperties bbsDataSourceProperties) {
		return bbsDataSourceProperties.initializeDataSourceBuilder().build();
	}
	
	@Bean
	@ConfigurationProperties("datasource.blog")
	public DataSourceProperties blogDataSourceProperties() {
		return new DataSourceProperties();
	}
	
	@Bean
	public DataSource blogDataSource(DataSourceProperties blogDataSourceProperties) {
		return blogDataSourceProperties.initializeDataSourceBuilder().build();
	}

}
