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
 * 아무것도 없는 단순한 job
 * 첫 번째 실행 이후 실행은 동일 job으로 간주되어 실행되지 않음
 */
@Slf4j
@Configuration
public class SimpleJob1Config {
	
	@Bean
	Job simpleJob1(JobRepository jobRepository, Step sampleJob1Step) {
	    return new JobBuilder("simpleJob1", jobRepository)
	    		.start(sampleJob1Step)
	    		.build();
	}

	@Bean
	Step sampleJob1Step(JobRepository jobRepository, PlatformTransactionManager transactionManager) { 
		return new StepBuilder("sampleJob1Step", jobRepository)
				.tasklet((StepContribution contribution, ChunkContext chunkContext) -> {
					log.debug("sampleJob1Step");
					return RepeatStatus.FINISHED;
					}, transactionManager)
				.build();
	}
}
