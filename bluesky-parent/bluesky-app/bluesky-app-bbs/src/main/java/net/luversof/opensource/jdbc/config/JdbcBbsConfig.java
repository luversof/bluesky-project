package net.luversof.opensource.jdbc.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "classpath:jdbc-bbs.properties", ignoreResourceNotFound = true)
@PropertySource(value = "classpath:jdbc-bbs-${spring.profiles.active}.properties", ignoreResourceNotFound = true)
public class JdbcBbsConfig {
	
	@Bean
	@ConfigurationProperties("datasource.bbs")
	public DataSourceProperties bbsDataSourceProperties() {
		return new DataSourceProperties();
	}
	
	@Bean
	public DataSource bbsDataSource(@Qualifier("bbsDataSourceProperties") DataSourceProperties dataSourceProperties) {
		return dataSourceProperties.initializeDataSourceBuilder().build();
	}
}
