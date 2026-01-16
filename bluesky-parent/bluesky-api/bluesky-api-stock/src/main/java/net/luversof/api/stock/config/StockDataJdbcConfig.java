package net.luversof.api.stock.config;

import java.util.List;

import javax.sql.DataSource;

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
@EnableJdbcRepositories(basePackages = { "net.luversof.api.stock.**", "net.luversof.app.google.**" }, transactionManagerRef = "stockTransactionManager")
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

	@Bean
	JdbcCustomConversions stockJdbcCustomConversions() {
		return new JdbcCustomConversions(List.of(
				// new MapToStringConverter(),
				// new StringToMapConverter()
				new MapToPGobjectConverter(),
				new PGobjectToMapConverter()));
	}
	
}
