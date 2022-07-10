package net.luversof.opensource.jdbc.config;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "classpath:jdbc-user.properties", ignoreResourceNotFound = true)
@PropertySource(value = "classpath:jdbc-user-${net-profile}.properties", ignoreResourceNotFound = true)
public class JdbcUserConfig {

	@Bean
	@ConfigurationProperties("datasource.user")
	public DataSourceProperties userDataSourceProperties() {
		return new DataSourceProperties();
	}
	
	@Bean
	public DataSource userDataSource() {
		return userDataSourceProperties().initializeDataSourceBuilder().build();
	}
}
