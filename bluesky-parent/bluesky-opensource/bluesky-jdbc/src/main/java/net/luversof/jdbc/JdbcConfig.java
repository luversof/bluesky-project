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
	
	@Value("${dataSource.bookkeeping.password}") String passwordtest;
	
	private HikariConfig defaultConfig() {
		HikariConfig hikariConfig = new HikariConfig();
		hikariConfig.setMaximumPoolSize(15);
		hikariConfig.setConnectionTimeout(1000);
		hikariConfig.setInitializationFailFast(false);
		hikariConfig.setConnectionTestQuery("SELECT 1");
		return hikariConfig;
	}
	
	@Bean(destroyMethod="close")
	public DataSource blogDataSource(@Value("${dataSource.blog.serverName}") String serverName, @Value("${dataSource.blog.port}") int port, @Value("${dataSource.blog.catalog}") String catalog, @Value("${dataSource.blog.user}") String user, @Value("${dataSource.blog.password}") String password) {
		HikariConfig hikariConfig = defaultConfig();
		hikariConfig.setDataSourceClassName(mysqlDriverClassName);
		hikariConfig.setCatalog(catalog);
		hikariConfig.setUsername(user);
		hikariConfig.setPassword(password);
		hikariConfig.addDataSourceProperty("serverName", serverName);
		hikariConfig.addDataSourceProperty("port", port);
		return new HikariDataSource(hikariConfig);
	}

	@Bean(destroyMethod="close")
	public DataSource securityDataSource(@Value("${dataSource.security.serverName}") String serverName, @Value("${dataSource.security.port}") int port, @Value("${dataSource.security.catalog}") String catalog, @Value("${dataSource.security.user}") String user, @Value("${dataSource.security.password}") String password) {
		HikariConfig hikariConfig = defaultConfig();
		hikariConfig.setDataSourceClassName(mysqlDriverClassName);
		hikariConfig.setCatalog(catalog);
		hikariConfig.setUsername(user);
		hikariConfig.setPassword(password);
		hikariConfig.addDataSourceProperty("serverName", serverName);
		hikariConfig.addDataSourceProperty("port", port);
		return new HikariDataSource(hikariConfig);
	}

	@Bean(destroyMethod="close")
	public DataSource bookkeepingDataSource(@Value("${dataSource.bookkeeping.serverName}") String serverName, @Value("${dataSource.bookkeeping.port}") int port, @Value("${dataSource.bookkeeping.catalog}") String catalog, @Value("${dataSource.bookkeeping.user}") String user, @Value("${dataSource.bookkeeping.password}") String password) {
		HikariConfig hikariConfig = defaultConfig();
		hikariConfig.setDataSourceClassName(mysqlDriverClassName);
		hikariConfig.setCatalog(catalog);
		hikariConfig.setUsername(user);
		hikariConfig.setPassword(password);
		hikariConfig.addDataSourceProperty("serverName", serverName);
		hikariConfig.addDataSourceProperty("port", port);
		return new HikariDataSource(hikariConfig);
	}

}
