package net.luversof.batch.example.config;

import javax.sql.DataSource;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.batch.BatchDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.support.JdbcTransactionManager;

import com.zaxxer.hikari.HikariDataSource;

import io.github.luversof.boot.connectioninfo.ConnectionInfoCollector;

@Configuration
//@EnableBatchProcessing(dataSourceRef = "batchDataSource", transactionManagerRef = "batchTransactionManager")
public class BatchExampleConfig /* extends DefaultBatchConfiguration */ {

	@Bean
	@BatchDataSource
	DataSource batchDataSource(ConnectionInfoCollector<HikariDataSource> mariaDbDataSourceConnectionInfoCollector) {
		return mariaDbDataSourceConnectionInfoCollector.getConnectionInfoMap().get("spring_batch");
	}
	

	@Bean
	JdbcTransactionManager batchTransactionManager(@Qualifier("batchDataSource") DataSource batchDataSource) {
		return new JdbcTransactionManager(batchDataSource);
	}
}
