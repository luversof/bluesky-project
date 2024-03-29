package net.luversof.batch.example.config;

import javax.sql.DataSource;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.batch.BatchDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.support.JdbcTransactionManager;

import com.zaxxer.hikari.HikariDataSource;

import io.github.luversof.boot.connectioninfo.ConnectionInfoCollector;

@Configuration
public class BatchExampleConfig {

	@Bean
	@BatchDataSource
	DataSource batchDataSource(ConnectionInfoCollector<HikariDataSource> mariaDbDataSourceConnectionInfoCollector) {
		return mariaDbDataSourceConnectionInfoCollector.getConnectionInfoMap().get("spring_batch");
	}
	

	@Bean
	JdbcTransactionManager batchTransactionManager(@Qualifier("batchDataSource") DataSource batchDataSource) {
		return new JdbcTransactionManager(batchDataSource);
	}
	
	@Bean
	JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor(JobRegistry jobRegistry) {
		JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor = new JobRegistryBeanPostProcessor();
		jobRegistryBeanPostProcessor.setJobRegistry(jobRegistry);
		return jobRegistryBeanPostProcessor;
	}
}
