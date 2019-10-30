package net.luversof.sql.analyzer.opensource.jdbc.config;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "classpath:jdbc-sql-analyzer.properties", ignoreResourceNotFound = true)
@PropertySource(value = "classpath:jdbc-sql-analyzer-${spring.profiles.active}.properties", ignoreResourceNotFound = true)
public class JdbcSqlAnalyzerConfig {
	
	@Bean
	@ConfigurationProperties("datasource.sql.anaylzer")
	public DataSourceProperties sqlAnalyzerDataSourceProperties() {
		return new DataSourceProperties();
	}
	
	@Bean
	public DataSource sqlAnalyzerDataSource() {
		return sqlAnalyzerDataSourceProperties().initializeDataSourceBuilder().build();
	}
}

