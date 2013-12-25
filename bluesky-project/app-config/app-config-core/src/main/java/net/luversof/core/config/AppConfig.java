package net.luversof.core.config;

import net.luversof.core.Banner;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;
import org.springframework.stereotype.Controller;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass=true)
@ComponentScan(basePackages = "net.luversof", excludeFilters = @Filter(type = FilterType.ANNOTATION, value = { Controller.class }))
public class AppConfig {
	public AppConfig() {
		super();
		Banner.write(System.out, "app-config-core");
	}
}