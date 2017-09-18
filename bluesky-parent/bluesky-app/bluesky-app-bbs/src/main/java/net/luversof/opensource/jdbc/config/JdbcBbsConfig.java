package net.luversof.opensource.jdbc.config;

import javax.sql.DataSource;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "classpath:jdbc-bbs.properties", ignoreResourceNotFound = true)
@PropertySource(value = "classpath:jdbc-bbs-${spring.profiles.active}.properties", ignoreResourceNotFound = true)
public class JdbcBbsConfig {
	
	@Bean
	@ConfigurationProperties(prefix = "datasource.bbs")
	public DataSource bbsDataSource() {
		return DataSourceBuilder.create().build();
	}
}
