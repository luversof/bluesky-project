package net.luversof.api.board.config;



import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableJdbcRepositories(basePackages = "net.luversof.api.board.**.repository", jdbcOperationsRef = "boardNamedParameterJdbcOperations", transactionManagerRef = "boardTransactionManager")
public class BoardDataJdbcConfig {

	NamedParameterJdbcOperations boardNamedParameterJdbcOperations(@Qualifier("routingDataSource") DataSource routingDataSource) {
		return new NamedParameterJdbcTemplate(routingDataSource);
	}

	@Bean
	PlatformTransactionManager boardTransactionManager(@Qualifier("routingDataSource") DataSource routingDataSource) {
		return new DataSourceTransactionManager(routingDataSource);
	}

}