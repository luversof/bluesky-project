package net.luversof.opensource.jdbc.config;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "classpath:blog-jdbc.properties", ignoreResourceNotFound = true)
@PropertySource(value = "classpath:blog-jdbc-${spring.profiles.active}.properties", ignoreResourceNotFound = true)
public class BlogJdbcConfig {
	
	@Bean
	@ConfigurationProperties("datasource.blog")
	public DataSourceProperties blogDataSourceProperties() {
		return new DataSourceProperties();
	}
	
	@Bean
	public DataSource blogDataSource() {
		return blogDataSourceProperties().initializeDataSourceBuilder().build();
	}
}
