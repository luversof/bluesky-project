package net.luversof.api.board.config;



import java.util.Arrays;
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

import net.luversof.api.board.convert.converter.MapToPGobjectConverter;
import net.luversof.api.board.convert.converter.MapToStringConverter;
import net.luversof.api.board.convert.converter.PGobjectToMapConverter;
import net.luversof.api.board.convert.converter.StringToMapConverter;
import net.luversof.api.board.convert.converter.TimestampToZonedDateTimeConverter;
import net.luversof.api.board.convert.converter.ZonedDateTimeToTimestampConverter;
import net.luversof.api.board.convert.util.DataJdbcConverterUtil;

@Configuration
@EnableJdbcAuditing
@EnableJdbcRepositories(basePackages = "net.luversof.api.board.**.repository", jdbcOperationsRef = "boardNamedParameterJdbcOperations", transactionManagerRef = "boardTransactionManager")
public class BoardDataJdbcConfig {

	@Bean
	NamedParameterJdbcOperations boardNamedParameterJdbcOperations(@Qualifier("routingDataSource") DataSource routingDataSource) {
		return new NamedParameterJdbcTemplate(routingDataSource);
	}

	@Bean
	PlatformTransactionManager boardTransactionManager(@Qualifier("routingDataSource") DataSource routingDataSource) {
		return new DataSourceTransactionManager(routingDataSource);
	}
	
	
	@Bean
	<T> BeforeConvertCallback<T> boardBeforeConvertCallback() {
		return DataJdbcConverterUtil::prepareEntity;
	}
	
	// mariadb 사용시 converter 등록
	@Bean
	JdbcCustomConversions boardjdbcCustomConversions() {
		return new JdbcCustomConversions(List.of(
			new ZonedDateTimeToTimestampConverter(),
			new TimestampToZonedDateTimeConverter(),
//			new MapToStringConverter(),
//			new StringToMapConverter(),
			new MapToPGobjectConverter(),
			new PGobjectToMapConverter()
		));
	}

}