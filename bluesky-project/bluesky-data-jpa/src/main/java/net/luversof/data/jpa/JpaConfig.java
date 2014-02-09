package net.luversof.data.jpa;

import net.luversof.core.Banner;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass=true)
@ComponentScan
public class JpaConfig {
	public JpaConfig() {
		super();
		Banner.write(System.out, "bluesky-data-jpa");
	}
}