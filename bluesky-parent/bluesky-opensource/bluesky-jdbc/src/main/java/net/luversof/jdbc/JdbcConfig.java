package net.luversof.jdbc;

import javax.sql.DataSource;

import net.luversof.core.Banner;
import net.luversof.core.BlueskyCoreConfig;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@ComponentScan
@Configuration
@Import(BlueskyCoreConfig.class)
@EnableAspectJAutoProxy(proxyTargetClass = true)
@PropertySource(name = "jdbcProp", value = "classpath:config/jdbc.properties")
@PropertySource(name = "jdbcActiveProp", value = "classpath:config/jdbc-${spring.profiles.active}.properties")
public class JdbcConfig {
	
	public JdbcConfig() {
		Banner.write(this);
	}
	
	@Value("${dataSource.mysql.driverClassName}")
	private String mysqlDriverClassName;
	
	private HikariConfig defaultConfig() {
		HikariConfig hikariConfig = new HikariConfig();
		hikariConfig.setMaximumPoolSize(3);
		hikariConfig.setMaxLifetime(1000);
		hikariConfig.setIdleTimeout(1000);
		hikariConfig.setConnectionTimeout(1000);
		hikariConfig.setInitializationFailFast(true);
		hikariConfig.setDataSourceClassName(mysqlDriverClassName);
		hikariConfig.setConnectionTestQuery("SELECT 1");
		return hikariConfig;
	}
	
	@Bean
	public DataSource blogDataSource(@Value("${dataSource.blog.serverName}") String serverName, @Value("${dataSource.blog.port}") int port, @Value("${dataSource.blog.databaseName}") String databaseName, @Value("${dataSource.blog.user}") String user, @Value("${dataSource.blog.password}") String password) {
		HikariConfig hikariConfig = defaultConfig();
		hikariConfig.addDataSourceProperty("serverName", serverName);
		hikariConfig.addDataSourceProperty("port", port);
		hikariConfig.addDataSourceProperty("databaseName", databaseName);
		hikariConfig.addDataSourceProperty("user", user);
		hikariConfig.addDataSourceProperty("password", password);
		return new HikariDataSource(hikariConfig);
	}

	@Bean
	public DataSource securityDataSource(@Value("${dataSource.security.serverName}") String serverName, @Value("${dataSource.security.port}") int port, @Value("${dataSource.security.databaseName}") String databaseName, @Value("${dataSource.security.user}") String user, @Value("${dataSource.security.password}") String password) {
		HikariConfig hikariConfig = defaultConfig();
		hikariConfig.setDataSourceClassName(mysqlDriverClassName);
		hikariConfig.addDataSourceProperty("serverName", serverName);
		hikariConfig.addDataSourceProperty("port", port);
		hikariConfig.addDataSourceProperty("databaseName", databaseName);
		hikariConfig.addDataSourceProperty("user", user);
		hikariConfig.addDataSourceProperty("password", password);
		return new HikariDataSource(hikariConfig);
	}

	@Bean
	public DataSource bookkeepingDataSource(@Value("${dataSource.bookkeeping.serverName}") String serverName, @Value("${dataSource.bookkeeping.port}") int port, @Value("${dataSource.bookkeeping.databaseName}") String databaseName, @Value("${dataSource.bookkeeping.user}") String user, @Value("${dataSource.bookkeeping.password}") String password) {
		HikariConfig hikariConfig = defaultConfig();
		hikariConfig.setDataSourceClassName(mysqlDriverClassName);
		hikariConfig.addDataSourceProperty("serverName", serverName);
		hikariConfig.addDataSourceProperty("port", port);
		hikariConfig.addDataSourceProperty("databaseName", databaseName);
		hikariConfig.addDataSourceProperty("user", user);
		hikariConfig.addDataSourceProperty("password", password);
		return new HikariDataSource(hikariConfig);
	}

}
