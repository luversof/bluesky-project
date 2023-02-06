package net.luversof.api.board.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
	public class BoardJdbcConfig {

    @Bean
    @ConfigurationProperties("datasource.board")
    DataSourceProperties boardDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    DataSource boardDataSource(@Qualifier("boardDataSourceProperties") DataSourceProperties dataSourceProperties) {
        return dataSourceProperties.initializeDataSourceBuilder().build();
    }
}
