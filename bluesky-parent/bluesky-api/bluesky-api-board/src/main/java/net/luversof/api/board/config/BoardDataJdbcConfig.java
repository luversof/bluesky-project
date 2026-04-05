package net.luversof.api.board.config;

import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.data.relational.core.mapping.event.BeforeConvertCallback;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import io.github.luversof.boot.data.convert.MapToPGobjectConverter;
import io.github.luversof.boot.data.convert.PGobjectToMapConverter;
import io.github.luversof.boot.data.convert.jdbc.util.DataJdbcConverterUtil;

@Configuration
@EnableJdbcAuditing
@EnableJdbcRepositories(
    basePackages = "net.luversof.api.board.**.repository",
    transactionManagerRef = "boardTransactionManager")
public class BoardDataJdbcConfig {

  @Bean
  JdbcClient boardJdbcClient(@Qualifier("routingDataSource") DataSource routingDataSource) {
    return JdbcClient.create(routingDataSource);
  }

  @Bean
  PlatformTransactionManager boardTransactionManager(
      @Qualifier("routingDataSource") DataSource routingDataSource) {
    return new DataSourceTransactionManager(routingDataSource);
  }

  @Bean
  <T> BeforeConvertCallback<T> boardBeforeConvertCallback() {
    return DataJdbcConverterUtil::prepareEntity;
  }

  @Bean
  JdbcCustomConversions boardJdbcCustomConversions() {
    return new JdbcCustomConversions(
        List.of(
            // new MapToStringConverter(),
            // new StringToMapConverter()
            new MapToPGobjectConverter(), new PGobjectToMapConverter()));
  }
}
