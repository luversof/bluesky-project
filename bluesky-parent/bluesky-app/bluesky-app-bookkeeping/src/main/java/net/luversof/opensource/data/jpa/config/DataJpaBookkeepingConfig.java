package net.luversof.opensource.data.jpa.config;



import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableJpaRepositories(basePackages = "net.luversof.bookkeeping.**.repository", entityManagerFactoryRef = "bookkeepingEntityManagerFactory", transactionManagerRef = "bookkeepingTransactionManager")
public class DataJpaBookkeepingConfig {

    @Bean(name = "bookkeepingEntityManagerFactory")
    LocalContainerEntityManagerFactoryBean bookkeepingEntityManagerFactory(EntityManagerFactoryBuilder builder, @Qualifier("bookkeepingDataSource") DataSource bookkeepingDataSource) {
        return builder
                .dataSource(bookkeepingDataSource)
                .persistenceUnit("bookkeepingPersistenceUnit")
                .packages(Jsr310JpaConverters.class)
                .packages("net.luversof.bookkeeping.**.domain").build();
    }

    @Bean(name = "bookkeepingTransactionManager")
    PlatformTransactionManager bookkeepingTransactionManager(@Qualifier("bookkeepingEntityManagerFactory") LocalContainerEntityManagerFactoryBean bookkeepingEntityManagerFactory) {
        return new JpaTransactionManager(bookkeepingEntityManagerFactory.getObject());
    }
}