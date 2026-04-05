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

  @Bean
  JdbcClient userJdbcClient(@Qualifier("routingDataSource") DataSource routingDataSource) {
    return JdbcClient.create(routingDataSource);
  }

  @Bean
  PlatformTransactionManager userTransactionManager(
      @Qualifier("routingDataSource") DataSource routingDataSource) {
    return new DataSourceTransactionManager(routingDataSource);
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
