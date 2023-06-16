package net.luversof.api.user.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.jdbc.JdbcProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@PropertySource(value = "classpath:jdbc-user.properties", ignoreResourceNotFound = true)
@PropertySource(value = "classpath:jdbc-user-${bluesky-boot-profile}.properties", ignoreResourceNotFound = true)
public class UserJdbcConfig {

	@Bean
	@ConfigurationProperties("datasource.user")
	DataSourceProperties userDataSourceProperties() {
		return new DataSourceProperties();
	}
	
	@Bean
	DataSource userDataSource() {
		return userDataSourceProperties().initializeDataSourceBuilder().build();
	}
	
	@Bean
	JdbcTemplate jdbcTemplate(@Qualifier("userDataSource") DataSource dataSource, JdbcProperties properties) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		JdbcProperties.Template template = properties.getTemplate();
		jdbcTemplate.setFetchSize(template.getFetchSize());
		jdbcTemplate.setMaxRows(template.getMaxRows());
		if (template.getQueryTimeout() != null) {
			jdbcTemplate.setQueryTimeout((int) template.getQueryTimeout().getSeconds());
		}
		return jdbcTemplate;
	}
}
