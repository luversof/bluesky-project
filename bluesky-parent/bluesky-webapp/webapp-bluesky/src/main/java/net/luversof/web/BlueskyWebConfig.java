package net.luversof.web;

import java.nio.charset.StandardCharsets;

import net.luversof.core.Banner;
import net.luversof.core.BlueskyCoreConfig;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

@Configuration
@ComponentScan
@Import(BlueskyCoreConfig.class)
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class BlueskyWebConfig {
	
	public BlueskyWebConfig() {
		Banner.write(this);
	}

	@Bean
	public MessageSource messageSource() {
		ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
		messageSource.setBasenames("public-resources/message/message", "public-resources/message/url");
		messageSource.setCacheSeconds(5);
		messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
		return messageSource;
	}
}
