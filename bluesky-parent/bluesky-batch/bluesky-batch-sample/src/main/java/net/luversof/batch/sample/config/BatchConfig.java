package net.luversof.batch.sample.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBatchProcessing
public class BatchConfig {
	
//	@Autowired
//	private BatchProperties batchProperties;
//
//	@Bean
//	public BatchDataSourceInitializer batchDataSourceInitializer(@Qualifier("batchDataSource") DataSource batchDataSource, ResourceLoader resourceLoader) {
//		return new BatchDataSourceInitializer(batchDataSource, resourceLoader, this.batchProperties);
//	}
}
