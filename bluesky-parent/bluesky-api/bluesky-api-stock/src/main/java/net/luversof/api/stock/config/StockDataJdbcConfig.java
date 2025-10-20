package net.luversof.api.stock.config;

import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.data.relational.core.mapping.event.BeforeConvertCallback;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import io.github.luversof.boot.data.convert.MapToPGobjectConverter;
import io.github.luversof.boot.data.convert.PGobjectToMapConverter;
import io.github.luversof.boot.data.convert.jdbc.util.DataJdbcConverterUtil;

@Configuration
@EnableJdbcAuditing
@EnableJdbcRepositories(basePackages = "net.luversof.api.stock.**.repository", jdbcOperationsRef = "stockNamedParameterJdbcOperations", transactionManagerRef = "stockTransactionManager")
public class StockDataJdbcConfig {
	
	@Bean
	NamedParameterJdbcOperations stockNamedParameterJdbcOperations(@Qualifier("routingDataSource") DataSource routingDataSource) {
		return new NamedParameterJdbcTemplate(routingDataSource);
	}

	@Bean
	PlatformTransactionManager stockTransactionManager(@Qualifier("routingDataSource") DataSource routingDataSource) {
		return new DataSourceTransactionManager(routingDataSource);
	}
	
	@Bean
	<T> BeforeConvertCallback<T> stockBeforeConvertCallback() {
		return DataJdbcConverterUtil::prepareEntity;
	}
	
	@Bean
	JdbcCustomConversions stockJdbcCustomConversions() {
		return new JdbcCustomConversions(List.of(
//			new MapToStringConverter(),
//			new StringToMapConverter()
			new MapToPGobjectConverter(),
			new PGobjectToMapConverter()
		));
	}

}
