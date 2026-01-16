package net.luversof.app.google.config;

import java.util.List;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.data.relational.core.mapping.event.BeforeConvertCallback;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import io.github.luversof.boot.connectioninfo.ConnectionInfoUtil;
import io.github.luversof.boot.data.convert.MapToPGobjectConverter;
import io.github.luversof.boot.data.convert.PGobjectToMapConverter;
import io.github.luversof.boot.data.convert.jdbc.util.DataJdbcConverterUtil;

@Configuration
// @EnableJdbcAuditing
public class GoogleAppDataJdbcConfig {
	
	private DataSource getDataSource() {
		return ConnectionInfoUtil.getConnection("google_api_postgresql");
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
	@ConditionalOnMissingBean
	JdbcCustomConversions googleAppJdbcCustomConversions() {
		return new JdbcCustomConversions(List.of(
				new MapToPGobjectConverter(),
				new PGobjectToMapConverter()));
	}

}
