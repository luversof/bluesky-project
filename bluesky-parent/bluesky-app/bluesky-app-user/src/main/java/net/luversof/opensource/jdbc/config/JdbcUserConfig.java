package net.luversof.opensource.jdbc.config;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
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
	@ConfigurationProperties(prefix = "datasource.user")
	public DataSource userDataSource() {
		return DataSourceBuilder.create().build();
	}
}
