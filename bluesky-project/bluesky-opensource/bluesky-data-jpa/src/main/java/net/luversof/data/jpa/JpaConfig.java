package net.luversof.data.jpa;

import net.luversof.core.Banner;
import net.luversof.core.BlueskyCoreConfig;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

@Configuration
@ComponentScan
@Import(BlueskyCoreConfig.class)
@PropertySources(value =  @PropertySource(name = "jpaProp", value = "classpath:config/repository-${spring.profiles.active}.properties"))
public class JpaConfig {
	public JpaConfig() {
		Banner.write(System.out, this.getClass().getPackage().getName());
	}
}