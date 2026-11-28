package net.luversof.api.bookkeeping.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
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

  /*
   * 저장소·쿼리·트랜잭션 매니저가 모두 같은 bookkeepingDataSource(connection-info 가 connections.bookkeeping alias 로 등록하는 빈)를 쓴다.
   * Spring 은 DataSource 객체를 key 로 트랜잭션 연결을 묶으므로 한쪽만 다른 DataSource 를 쓰면 트랜잭션 밖 연결에서 실행된다. 아래
   * 빈이 있으면 Spring Boot 의 같은 타입 빈은 만들어지지 않으므로 저장소와 이름 없이 주입받는 곳도 이 빈을 받는다.
   * jdbcOperationsRef 는 지정하지 않는다: 지정하면 저장소가 BeforeConvertCallback 을 거치지 않는다.
   */
  @Bean
  JdbcTemplate bookkeepingJdbcTemplate(@Qualifier("bookkeepingDataSource") DataSource dataSource) {
    return new JdbcTemplate(dataSource);
  }

  @Bean
  NamedParameterJdbcTemplate bookkeepingNamedParameterJdbcTemplate(
      JdbcTemplate bookkeepingJdbcTemplate) {
    return new NamedParameterJdbcTemplate(bookkeepingJdbcTemplate);
  }

  @Bean
  JdbcClient bookkeepingJdbcClient(@Qualifier("bookkeepingDataSource") DataSource dataSource) {
    return JdbcClient.create(dataSource);
  }

  @Bean
  PlatformTransactionManager bookkeepingTransactionManager(
      @Qualifier("bookkeepingDataSource") DataSource dataSource) {
    return new DataSourceTransactionManager(dataSource);
  }
}
