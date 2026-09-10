package net.luversof.api.stock.config;

import java.util.List;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.data.jdbc.core.dialect.JdbcPostgresDialect;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.data.relational.core.mapping.event.BeforeConvertCallback;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import io.github.luversof.boot.connectioninfo.ConnectionInfoUtil;
import io.github.luversof.boot.data.convert.MapToPGobjectConverter;
import io.github.luversof.boot.data.convert.PGobjectToMapConverter;
import io.github.luversof.boot.data.convert.jdbc.util.DataJdbcConverterUtil;

@Configuration
@EnableJdbcAuditing
@EnableJdbcRepositories(
    basePackages = {"net.luversof.api.stock.**", "net.luversof.app.google.**"},
    transactionManagerRef = "stockTransactionManager")
public class StockDataJdbcConfig {

  private DataSource getDataSource() {
    return ConnectionInfoUtil.getConnection("stock_postgresql");
  }

  @Bean
  JdbcClient stockJdbcClient() {
    return JdbcClient.create(getDataSource());
  }

  @Bean
  PlatformTransactionManager stockTransactionManager() {
    return new DataSourceTransactionManager(getDataSource());
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
        List.of(
            // new MapToStringConverter(),
            // new StringToMapConverter()
            new MapToPGobjectConverter(), new PGobjectToMapConverter()));
  }
}
