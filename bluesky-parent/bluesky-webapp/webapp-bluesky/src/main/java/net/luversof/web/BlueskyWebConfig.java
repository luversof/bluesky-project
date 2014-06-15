package net.luversof.web;

import net.luversof.core.Banner;
import net.luversof.core.BlueskyCoreConfig;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan
@Import(BlueskyCoreConfig.class)
public class BlueskyWebConfig {
	public BlueskyWebConfig() {
		Banner.write(System.out, this.getClass().getPackage().getName());
	}
}
