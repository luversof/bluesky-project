package net.luversof.web.dynamiccrud.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class DynamicCrudDataJpaConfig {

	@Configuration
	@EnableJpaRepositories(basePackages = "net.luversof.web.dynamiccrud.setting.repository", entityManagerFactoryRef = "dynamicCrudSettingEntityManagerFactory", transactionManagerRef = "dynamicCrudSettingTransactionManager")
	public class DynamicCrudSettingDataJpaConfig {

	    @Bean(name = "dynamicCrudSettingEntityManagerFactory")
	    LocalContainerEntityManagerFactoryBean dynamicCrudSettingEntityManagerFactory(EntityManagerFactoryBuilder builder, @Qualifier("routingDataSource") DataSource routingDataSource) {
	        return builder
	                .dataSource(routingDataSource)
	                .persistenceUnit("dynamicCrudSettingPersistenceUnit")
	                .packages("net.luversof.web.dynamiccrud.setting.domain").build();
	    }

	    @Bean(name = "dynamicCrudSettingTransactionManager")
	    PlatformTransactionManager dynamicCrudSettingTransactionManager(@Qualifier("dynamicCrudSettingEntityManagerFactory") LocalContainerEntityManagerFactoryBean dynamicCrudSettingEntityManagerFactory) {
	        return new JpaTransactionManager(dynamicCrudSettingEntityManagerFactory.getObject());
	    }
	}
}
