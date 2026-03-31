package net.luversof.batch.example.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/** 아무것도 없는 단순한 job 첫 번째 실행 이후 실행은 동일 job으로 간주되어 실행되지 않음 */
@Configuration
public class SimpleJob1Config {

    private static final Logger log = LoggerFactory.getLogger(SimpleJob1Config.class);

    @Bean
    Job simpleJob1(JobRepository jobRepository, Step sampleJob1Step) {
        return new JobBuilder("simpleJob1", jobRepository).start(sampleJob1Step).build();
    }

    @Bean
    Step sampleJob1Step(
            JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("sampleJob1Step", jobRepository)
                .tasklet(
                        (StepContribution _, ChunkContext _) -> {
                            log.debug("sampleJob1Step");
                            return RepeatStatus.FINISHED;
                        },
                        transactionManager)
                .build();
    }
}
