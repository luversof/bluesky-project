package net.luversof.api.user.config;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.data.relational.core.mapping.event.BeforeConvertCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import io.github.luversof.boot.data.convert.MapToPGobjectConverter;
import io.github.luversof.boot.data.convert.PGobjectToMapConverter;
import io.github.luversof.boot.data.convert.TimestampToOffsetDateTimeConverter;
import io.github.luversof.boot.data.convert.jdbc.util.DataJdbcConverterUtil;

@Configuration
@EnableJdbcAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
@EnableJdbcRepositories(
    basePackages = "net.luversof.api.user.**.repository",
    transactionManagerRef = "userTransactionManager")
public class UserDataJdbcConfig {

  @Bean
  DateTimeProvider auditingDateTimeProvider() {
    return () -> Optional.of(OffsetDateTime.now());
  }

  /*
   * 저장소·쿼리·트랜잭션 매니저가 모두 같은 userDataSource(connection-info 가 connections.user alias 로 등록하는 빈)를 쓴다.
   * Spring 은 DataSource 객체를 key 로 트랜잭션 연결을 묶으므로 한쪽만 다른 DataSource 를 쓰면 트랜잭션 밖 연결에서 실행된다. 아래
   * 빈이 있으면 Spring Boot 의 같은 타입 빈은 만들어지지 않으므로 저장소와 이름 없이 주입받는 곳도 이 빈을 받는다.
   * jdbcOperationsRef 는 지정하지 않는다: 지정하면 저장소가 BeforeConvertCallback 을 거치지 않는다.
   */
  @Bean
  JdbcTemplate userJdbcTemplate(@Qualifier("userDataSource") DataSource dataSource) {
    return new JdbcTemplate(dataSource);
  }

  @Bean
  NamedParameterJdbcTemplate userNamedParameterJdbcTemplate(JdbcTemplate userJdbcTemplate) {
    return new NamedParameterJdbcTemplate(userJdbcTemplate);
  }

  @Bean
  JdbcClient userJdbcClient(@Qualifier("userDataSource") DataSource dataSource) {
    return JdbcClient.create(dataSource);
  }

  @Bean
  PlatformTransactionManager userTransactionManager(
      @Qualifier("userDataSource") DataSource dataSource) {
    return new DataSourceTransactionManager(dataSource);
  }

  @Bean
  <T> BeforeConvertCallback<T> userBeforeConvertCallback() {
    return DataJdbcConverterUtil::prepareEntity;
  }

  @Bean
  JdbcCustomConversions userJdbcCustomConversions() {
    return new JdbcCustomConversions(
        List.of(
            // new MapToStringConverter(),
            // new StringToMapConverter()
            new MapToPGobjectConverter(),
            new PGobjectToMapConverter(),
            new TimestampToOffsetDateTimeConverter()));
  }
}
