package net.luversof.web;

import net.luversof.core.Banner;
import net.luversof.core.BlueskyCoreConfig;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ComponentScan
@Import(BlueskyCoreConfig.class)
@EnableAspectJAutoProxy(proxyTargetClass = true)
@PropertySource(name = "urlProp", value = "public-resources/config/url.properties")
public class BlueskyWebConfig {
	
	public BlueskyWebConfig() {
		Banner.write(this);
	}
	
}
