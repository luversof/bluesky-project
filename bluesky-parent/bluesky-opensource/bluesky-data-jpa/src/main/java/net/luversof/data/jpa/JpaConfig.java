package net.luversof.data.jpa;

import net.luversof.core.Banner;
import net.luversof.core.BlueskyCoreConfig;
import net.luversof.jdbc.JdbcConfig;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ComponentScan
@Import({BlueskyCoreConfig.class, JdbcConfig.class})
@PropertySource(name = "jpaProp", value = "classpath:config/data-jpa-${spring.profiles.active}.properties")
public class JpaConfig {
	public JpaConfig() {
		Banner.write(System.out, this.getClass().getPackage().getName());
	}
}