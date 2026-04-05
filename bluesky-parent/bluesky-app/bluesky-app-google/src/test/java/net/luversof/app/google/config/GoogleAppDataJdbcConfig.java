package net.luversof.app.google.config;

import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
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
    basePackages = "net.luversof.app.google.**",
    transactionManagerRef = "googleAppTransactionManager",
    enableDefaultTransactions = false)
public class GoogleAppDataJdbcConfig {

  @Value("${bluesky-boot.connection-info.loaders.hikaridatasource.connections.google_api}")
  private String connectionKey;

  private DataSource getDataSource() {
    return ConnectionInfoUtil.getConnection(connectionKey);
  }

  @Bean
  JdbcClient googleAppJdbcClient() {
    return JdbcClient.create(getDataSource());
  }

  @Bean
  PlatformTransactionManager googleAppTransactionManager() {
    return new DataSourceTransactionManager(getDataSource());
  }

  @Bean
  <T> BeforeConvertCallback<T> googleAppBeforeConvertCallback() {
    return DataJdbcConverterUtil::prepareEntity;
  }

  @Bean
  JdbcCustomConversions googleAppJdbcCustomConversions() {
    return new JdbcCustomConversions(
        List.of(new MapToPGobjectConverter(), new PGobjectToMapConverter()));
  }
}
