package net.luversof.api.bookkeeping.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 가계부 data-jdbc 설정.
 *
 * <p>도메인이 record 라 id 를 리플렉션으로 채우는 BeforeConvertCallback(DataJdbcConverterUtil)은 쓰지 않는다. 서비스가 id 를
 * 넣고 새 행은 JdbcAggregateTemplate.insert, 수정은 repository.save 로 저장한다.
 */
@Configuration
@EnableJdbcRepositories(
    basePackages = "net.luversof.api.bookkeeping.**.repository",
    transactionManagerRef = "bookkeepingTransactionManager")
public class BookkeepingDataJdbcConfig {

  @Bean
  JdbcClient bookkeepingJdbcClient(@Qualifier("routingDataSource") DataSource routingDataSource) {
    return JdbcClient.create(routingDataSource);
  }

  @Bean
  PlatformTransactionManager bookkeepingTransactionManager(
      @Qualifier("routingDataSource") DataSource routingDataSource) {
    return new DataSourceTransactionManager(routingDataSource);
  }
}
