package net.luversof.batch.example.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class TestJob2Config {

	@Bean
	Job testJob2(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
	    return new JobBuilder("testJob2", jobRepository)
                     .start(sampleStep2(jobRepository, transactionManager))
                     .build();
	}

	@Bean
	Step sampleStep2(JobRepository jobRepository, PlatformTransactionManager transactionManager) { 
		return new StepBuilder("sampleStep2", jobRepository)
					.<String, String>chunk(10, transactionManager) 
					.reader(itemReader())
					.writer(itemWriter())
					.build();
	}
	
	public ItemReader<String> itemReader() {
		return new ItemReader<String>() {
			@Override
			public String read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
				return "test";
			}
		};
	}
	
	public ItemWriter<String> itemWriter() {
		return new ItemWriter<String>() {
			@Override
			public void write(Chunk<? extends String> chunk) throws Exception {
				chunk.iterator().forEachRemaining(s -> 
					System.out.println("String is : " +  s)
				);
			}
		};
	}
}