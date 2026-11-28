package net.luversof.api.stock.config;

import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.data.jdbc.core.dialect.JdbcPostgresDialect;
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
import io.github.luversof.boot.data.convert.jdbc.util.DataJdbcConverterUtil;

@Configuration
@EnableJdbcAuditing
@EnableJdbcRepositories(
    basePackages = {"net.luversof.api.stock.**", "net.luversof.app.google.**"},
    transactionManagerRef = "stockTransactionManager")
// jdbcOperationsRef 는 지정하지 않는다: 지정하면 저장소가 BeforeConvertCallback 을 거치지 않아 id 없이 INSERT 된다(실측
// 2026-11-29).
public class StockDataJdbcConfig {

  /*
   * 모든 JDBC 경로가 트랜잭션 매니저와 같은 stockDataSource(connection-info가 connections.stock alias로 등록하는 빈)를 쓴다.
   * Spring 은 DataSource 객체를 key 로 트랜잭션 연결을 묶는다. 저장소/쿼리가 @Primary routingDataSource 를 쓰면 key 가 달라
   * @Transactional 안의 쓰기가 다른 연결(autocommit)로 실행되어 롤백되지 않았다. 아래 두 빈이 있으면 Spring Boot 의 같은 타입 빈은
   * 만들어지지 않으므로 이름 없이 주입받는 곳(쿼리 클래스, StockAdminService)도 이 빈을 받는다.
   */
  @Bean
  JdbcTemplate stockJdbcTemplate(@Qualifier("stockDataSource") DataSource dataSource) {
    return new JdbcTemplate(dataSource);
  }

  @Bean
  NamedParameterJdbcTemplate stockNamedParameterJdbcTemplate(JdbcTemplate stockJdbcTemplate) {
    return new NamedParameterJdbcTemplate(stockJdbcTemplate);
  }

  @Bean
  JdbcClient stockJdbcClient(@Qualifier("stockDataSource") DataSource dataSource) {
    return JdbcClient.create(dataSource);
  }

  @Bean
  PlatformTransactionManager stockTransactionManager(
      @Qualifier("stockDataSource") DataSource dataSource) {
    return new DataSourceTransactionManager(dataSource);
  }

  @Bean
  <T> BeforeConvertCallback<T> stockBeforeConvertCallback() {
    return DataJdbcConverterUtil::prepareEntity;
  }

  /**
   * PostgreSQL dialect 의 단순 타입(PGobject 등)을 store conversions 에 넣어 만든다. 생성자 {@code new
   * JdbcCustomConversions(List)} 로 만들면 PGobject 가 저장소 타입이 아니라서 기동마다 CustomConversions WARN
   * 2줄(reading/writing converter ... doesn't convert from/to a store-supported type)이 났다(실측
   * 2026-09-09). 이 설정은 stock_postgresql 전용이라 dialect 를 고정한다.
   */
  @Bean
  JdbcCustomConversions stockJdbcCustomConversions() {
    return JdbcCustomConversions.of(
        JdbcPostgresDialect.INSTANCE,
        List.of(new MapToPGobjectConverter(), new PGobjectToMapConverter()));
  }
}
