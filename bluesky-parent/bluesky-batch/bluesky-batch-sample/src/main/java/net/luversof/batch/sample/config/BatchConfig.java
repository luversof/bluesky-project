package net.luversof.batch.sample.config;

import javax.sql.DataSource;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.batch.BatchDataSourceInitializer;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

@Configuration
@EnableBatchProcessing
public class BatchConfig {
	
	@Autowired
	private BatchProperties batchProperties;
//
//	@Bean
//	public BatchDataSourceInitializer batchDataSourceInitializer(@Qualifier("batchDataSource") DataSource batchDataSource, ResourceLoader resourceLoader) {
//		return new BatchDataSourceInitializer(batchDataSource, resourceLoader, this.batchProperties);
//	}
}
