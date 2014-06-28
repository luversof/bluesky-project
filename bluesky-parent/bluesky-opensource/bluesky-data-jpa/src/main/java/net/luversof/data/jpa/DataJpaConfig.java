package net.luversof.data.jpa;

import net.luversof.core.Banner;
import net.luversof.core.BlueskyCoreConfig;
import net.luversof.jdbc.JdbcConfig;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ComponentScan
@Import({ BlueskyCoreConfig.class, JdbcConfig.class })
@EnableJpaRepositories(basePackages = "net.luversof")
@EnableTransactionManagement(proxyTargetClass = true)
@EnableJpaAuditing
@PropertySource(name = "jpaProp", value = "classpath:config/data-jpa-${spring.profiles.active}.properties")
public class DataJpaConfig {

	public DataJpaConfig() {
		Banner.write(this);
	}

}