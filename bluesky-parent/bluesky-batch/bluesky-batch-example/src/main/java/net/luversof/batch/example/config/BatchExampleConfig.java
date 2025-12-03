package net.luversof.batch.example.config;

import java.util.NoSuchElementException;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.support.JdbcTransactionManager;

import com.zaxxer.hikari.HikariDataSource;

import io.github.luversof.boot.connectioninfo.ConnectionInfoRegistry;

@Configuration
public class BatchExampleConfig {

	@Bean
	DataSource batchDataSource(ConnectionInfoRegistry<HikariDataSource> connectionInfoRegistry) {
		var target = connectionInfoRegistry.getConnectionInfoList().stream().filter(x -> x.getKey().connectionKey().equals("spring_batch")).findFirst();
		if (target.isEmpty()) {
			throw new NoSuchElementException("No value present");
		}
		
		return target.get().getConnection();
	}
	

	@Bean
	JdbcTransactionManager batchTransactionManager(@Qualifier("batchDataSource") DataSource batchDataSource) {
		return new JdbcTransactionManager(batchDataSource);
	}
	
}
