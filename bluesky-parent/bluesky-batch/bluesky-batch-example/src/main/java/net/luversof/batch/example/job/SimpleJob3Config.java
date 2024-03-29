package net.luversof.batch.example.job;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import lombok.extern.slf4j.Slf4j;

/**
 * 매개변수를 받는 경우
 */
@Slf4j
@Configuration
public class SimpleJob3Config {
	
	@Bean
	Job simpleJob3(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
	    return new JobBuilder("simpleJob3", jobRepository)
	    		.start(sampleJob3Step(jobRepository, transactionManager))
	    		.build();
	}

	@Bean
	Step sampleJob3Step(JobRepository jobRepository, PlatformTransactionManager transactionManager) { 
		return new StepBuilder("sampleJob3Step", jobRepository)
				.tasklet((StepContribution contribution, ChunkContext chunkContext) -> {
					log.debug("sampleJob2Step");
					return RepeatStatus.FINISHED;
					}, transactionManager)
				.build();
	}
}
