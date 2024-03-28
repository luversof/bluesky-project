package net.luversof.batch.example.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class TestJobConfig {

	@Bean
	Job testJob(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
	    return new JobBuilder("testJob", jobRepository)
                     .start(sampleStep(jobRepository, transactionManager))
                     .build();
	}

	@Bean
	Step sampleStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) { 
		return new StepBuilder("sampleStep", jobRepository)
					.<String, String>chunk(2, transactionManager) 
					.reader(itemReader())
					.processor(itemProcessor())
					.writer(itemWriter())
					.build();
	}
	
	
	private int count = 0;
	
	public ItemReader<String> itemReader() {
		return new ItemReader<String>() {
			@Override
			public String read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
				if (count == 0) {
					count++;
					return "test";
				}
				return null;
			}
		};
	}
	
	public ItemProcessor<String, String> itemProcessor() {
		return new ItemProcessor<String, String>() {

			@Override
			public String process(String item) throws Exception {
				System.out.println("itemProcessor : " +  item);
				return item;
			}
		};
	}
	
	public ItemWriter<String> itemWriter() {
		return new ItemWriter<String>() {
			@Override
			public void write(Chunk<? extends String> chunk) throws Exception {
				System.out.println("itemWriter  : " +  chunk.getItems());
			}
		};
	}
}