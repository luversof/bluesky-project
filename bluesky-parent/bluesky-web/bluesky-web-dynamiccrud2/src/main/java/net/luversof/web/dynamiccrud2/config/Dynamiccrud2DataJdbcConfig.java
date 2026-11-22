package net.luversof.web.dynamiccrud2.config;

import java.util.List;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.data.jdbc.core.dialect.JdbcPostgresDialect;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.data.relational.core.mapping.event.BeforeConvertCallback;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import io.github.luversof.boot.connectioninfo.ConnectionInfoUtil;
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
public class Dynamiccrud2DataJdbcConfig {

  public static final String CONNECTION_NAME = "dynamiccrud2";

  private DataSource getDataSource() {
    return ConnectionInfoUtil.getConnection(CONNECTION_NAME, DataSource.class);
  }

  @Bean
  PlatformTransactionManager dynamiccrud2TransactionManager() {
    return new DataSourceTransactionManager(getDataSource());
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
