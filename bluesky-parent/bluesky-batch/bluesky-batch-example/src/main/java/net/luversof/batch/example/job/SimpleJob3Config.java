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

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 매개변수를 받는 경우 테스트
 * jobRepository랑transactionManager를 매개변수로 안넘기면 안되나?
 */
@Slf4j
@Configuration
@AllArgsConstructor
public class SimpleJob3Config {

	private JobRepository jobRepository;
	private PlatformTransactionManager transactionManager;
	
	@Bean
	Job simpleJob3() {
	    return new JobBuilder("simpleJob3", jobRepository)
	    		.start(sampleJob3Step())
	    		.build();
	}

	@Bean
	Step sampleJob3Step() { 
		return new StepBuilder("sampleJob3Step", jobRepository)
				.tasklet((StepContribution contribution, ChunkContext chunkContext) -> {
					log.debug("sampleJob2Step");
					return RepeatStatus.FINISHED;
					}, transactionManager)
				.build();
	}
}
