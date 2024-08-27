package net.luversof.api.bookkeeping.config;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BookkeepingJdbcConfig {

    @Bean
    @ConfigurationProperties("datasource.bookkeeping")
    DataSourceProperties bookkeepingDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    DataSource bookkeepingDataSource() {
        return bookkeepingDataSourceProperties().initializeDataSourceBuilder().build();
    }
    
    
    @Bean
    @ConfigurationProperties("datasource.bookkeeping-postgresql")
    DataSourceProperties bookkeepingPostgresqlDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    DataSource bookkeepingPostgresqlDataSource() {
        return bookkeepingPostgresqlDataSourceProperties().initializeDataSourceBuilder().build();
    }
}

