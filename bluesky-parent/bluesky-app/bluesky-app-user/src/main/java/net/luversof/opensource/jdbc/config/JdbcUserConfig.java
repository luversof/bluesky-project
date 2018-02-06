package net.luversof.opensource.jdbc.config;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "classpath:jdbc-user.properties", ignoreResourceNotFound = true)
@PropertySource(value = "classpath:jdbc-user-${spring.profiles.active}.properties", ignoreResourceNotFound = true)
public class JdbcUserConfig {

	
	@Bean
	@Primary
	@ConfigurationProperties("datasource.user")
	public DataSourceProperties userDataSourceProperties() {
		return new DataSourceProperties();
	}
	
	@Bean
	@Primary
	public DataSource userDataSource(DataSourceProperties userDataSourceProperties) {
		return userDataSourceProperties.initializeDataSourceBuilder().build();
	}
}
