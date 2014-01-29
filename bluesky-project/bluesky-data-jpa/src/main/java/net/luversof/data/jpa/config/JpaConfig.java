package net.luversof.data.jpa.config;

import net.luversof.data.jpa.Banner;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass=true)
@ComponentScan("net.luversof.data.jpa")
public class JpaConfig {
	public JpaConfig() {
		super();
		Banner.write(System.out, "bluesky-data-jpa");
	}
}