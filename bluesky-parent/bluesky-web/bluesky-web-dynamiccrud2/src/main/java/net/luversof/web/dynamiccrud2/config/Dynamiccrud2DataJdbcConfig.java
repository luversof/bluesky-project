package net.luversof.web.dynamiccrud2.config;

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
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import io.github.luversof.boot.data.convert.MapToPGobjectConverter;
import io.github.luversof.boot.data.convert.PGobjectToMapConverter;
import io.github.luversof.boot.data.convert.jdbc.util.DataJdbcConverterUtil;
import net.luversof.web.dynamiccrud2.domain.Menu;

/** 설정 catalog(dynamiccrud2, PostgreSQL) 저장소 설정. 대상 DB 는 여기서 다루지 않는다. */
@Configuration
@EnableJdbcAuditing
@EnableJdbcRepositories(
    basePackages = "net.luversof.web.dynamiccrud2.repository",
    transactionManagerRef = "dynamiccrud2TransactionManager")
// jdbcOperationsRef 는 지정하지 않는다: 지정하면 저장소가 BeforeConvertCallback 을 거치지 않아 id 없이 INSERT 된다(실측
// 2026-11-29).
public class Dynamiccrud2DataJdbcConfig {

  /*
   * 저장소·쿼리·트랜잭션 매니저가 모두 같은 dynamiccrud2settingDataSource(connection-info 가
   * connections.dynamiccrud2setting alias 로 등록하는 빈)를 쓴다. Spring 은 DataSource 객체를 key 로 트랜잭션 연결을 묶는다.
   * 저장소가 @Primary routingDataSource 를 쓰면 key 가 달라 저장소 작업이 트랜잭션 밖 연결(autocommit)에서 실행된다. 아래 빈이
   * 있으면 Spring Boot 의 같은 타입 빈은 만들어지지 않으므로 이름 없이 주입받는 *QueriesImpl 도 이 빈을 받는다.
   */
  @Bean
  JdbcTemplate dynamiccrud2JdbcTemplate(
      @Qualifier("dynamiccrud2settingDataSource") DataSource dataSource) {
    return new JdbcTemplate(dataSource);
  }

  @Bean
  NamedParameterJdbcTemplate dynamiccrud2NamedParameterJdbcTemplate(
      JdbcTemplate dynamiccrud2JdbcTemplate) {
    return new NamedParameterJdbcTemplate(dynamiccrud2JdbcTemplate);
  }

  @Bean
  PlatformTransactionManager dynamiccrud2TransactionManager(
      @Qualifier("dynamiccrud2settingDataSource") DataSource dataSource) {
    return new DataSourceTransactionManager(dataSource);
  }

  /** 루트 id 를 채우고, Menu 는 하위 MenuQuery id 도 채운다(ChangeLog 가 가리키므로). */
  @Bean
  <T> BeforeConvertCallback<T> dynamiccrud2BeforeConvertCallback() {
    return entity -> {
      DataJdbcConverterUtil.prepareEntity(entity);
      if (entity instanceof Menu menu) {
        menu.getQueries().forEach(DataJdbcConverterUtil::setId);
      }
      return entity;
    };
  }

  /** jsonb(paramSettings · bindValues) ↔ Map. 이 설정은 PostgreSQL 전용이라 dialect 를 고정한다. */
  @Bean
  JdbcCustomConversions dynamiccrud2JdbcCustomConversions() {
    return JdbcCustomConversions.of(
        JdbcPostgresDialect.INSTANCE,
        List.of(new MapToPGobjectConverter(), new PGobjectToMapConverter()));
  }
}
