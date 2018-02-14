package net.luversof.batch.sample.test1.config;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Test1JdbcConfig {

	@Bean
	@ConfigurationProperties("datasource.batch-test1")
	public DataSourceProperties batchTest1DataSourceProperties() {
		return new DataSourceProperties();
	}
	
	@Bean
	public DataSource batchTest1DataSource(DataSourceProperties batchTest1DataSourceProperties) {
		return batchTest1DataSourceProperties.initializeDataSourceBuilder().build();
	}
	
	@Bean
	@ConfigurationProperties("datasource.batch-test2")
	public DataSourceProperties batchTest2DataSourceProperties() {
		return new DataSourceProperties();
	}
	
	@Bean
	public DataSource batchTest2DataSource(DataSourceProperties batchTest2DataSourceProperties) {
		return batchTest2DataSourceProperties.initializeDataSourceBuilder().build();
	}
}
