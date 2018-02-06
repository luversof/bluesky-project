package net.luversof.opensource.jdbc.config;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "classpath:jdbc-blog.properties", ignoreResourceNotFound = true)
@PropertySource(value = "classpath:jdbc-blog-${spring.profiles.active}.properties", ignoreResourceNotFound = true)
public class JdbcBlogConfig {
	
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
