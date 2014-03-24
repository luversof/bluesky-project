package net.luversof.jdbc;

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
@EnableAspectJAutoProxy(proxyTargetClass=true)
@PropertySources(value =  @PropertySource(name = "jdbcProp", value = "classpath:config/jdbc-${spring.profiles.active}.properties"))
public class JdbcConfig {
	public JdbcConfig() {
		Banner.write(System.out, this.getClass().getPackage().getName());
	}
}
