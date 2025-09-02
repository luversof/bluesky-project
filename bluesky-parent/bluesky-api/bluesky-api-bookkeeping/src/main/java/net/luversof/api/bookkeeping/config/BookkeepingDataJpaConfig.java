package net.luversof.api.bookkeeping.config;

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
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import io.github.luversof.boot.data.convert.MapToStringConverter;
import io.github.luversof.boot.data.convert.StringToMapConverter;

@Configuration
@EnableJdbcAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
@EnableJdbcRepositories(basePackages = "net.luversof.api.bookkeeping.**.repository", jdbcOperationsRef = "bookkeepingNamedParameterJdbcOperations", transactionManagerRef = "bookkeepingTransactionManager")
public class BookkeepingDataJpaConfig {
	
	@Bean
	DateTimeProvider auditingDateTimeProvider() {
		return () -> Optional.of(OffsetDateTime.now());
	}
	
	@Bean
	NamedParameterJdbcOperations bookkeepingNamedParameterJdbcOperations(@Qualifier("routingDataSource") DataSource routingDataSource) {
		return new NamedParameterJdbcTemplate(routingDataSource);
	}

	@Bean
	PlatformTransactionManager bookkeepingTransactionManager(@Qualifier("routingDataSource") DataSource routingDataSource) {
		return new DataSourceTransactionManager(routingDataSource);
	}
	
	@Bean
	JdbcCustomConversions boardjdbcCustomConversions() {
		return new JdbcCustomConversions(List.of(
			new MapToStringConverter(),
			new StringToMapConverter()
//			new MapToPGobjectConverter(),
//			new PGobjectToMapConverter()
		));
	}

}
