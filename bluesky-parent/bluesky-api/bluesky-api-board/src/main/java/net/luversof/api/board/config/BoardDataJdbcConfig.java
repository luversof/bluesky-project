package net.luversof.api.board.config;



import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.data.relational.core.mapping.event.BeforeConvertCallback;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import io.github.luversof.boot.uuid.UuidGeneratorUtil;
import net.luversof.api.board.domain.Board;

@Configuration
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
	BeforeConvertCallback<Board> boardBeforeConvertCallback() {
		return board -> {
			if (board.getId() == null) {
				board.setId(UuidGeneratorUtil.getUuid());
			}
			return board;
		}; 
	}

}