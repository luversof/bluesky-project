package net.luversof.batch.example.job;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import lombok.extern.slf4j.Slf4j;

/**
 * 증가 처리 추가한 경우 예제
 * job 실행 시 매번 실행된다.
 */
@Slf4j
@Configuration
public class SimpleJob2Config {
	
	@Bean
	Job simpleJob2(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
	    return new JobBuilder("simpleJob2", jobRepository)
	    		
	    		.incrementer(new RunIdIncrementer())
	    		.start(sampleJob2Step(jobRepository, transactionManager))
	    		.build();
	}

	@Bean
	Step sampleJob2Step(JobRepository jobRepository, PlatformTransactionManager transactionManager) { 
		return new StepBuilder("sampleJob2Step", jobRepository)
				.tasklet((StepContribution contribution, ChunkContext chunkContext) -> {
					log.debug("sampleJob2Step");
					return RepeatStatus.FINISHED;
					}, transactionManager)
				.build();
	}

}
