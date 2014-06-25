package net.luversof.web;

import net.luversof.core.Banner;
import net.luversof.core.BlueskyCoreConfig;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan
@Import(BlueskyCoreConfig.class)
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class BlueskyWebConfig {
	public BlueskyWebConfig() {
		Banner.write(this);
	}
}
