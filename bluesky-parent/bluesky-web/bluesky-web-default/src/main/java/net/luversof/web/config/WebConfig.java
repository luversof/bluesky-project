package net.luversof.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.ThemeResolver;
import org.springframework.web.servlet.theme.CookieThemeResolver;

@Configuration
public class WebConfig {

	@Bean
	public ThemeResolver themeResolver() {
		return new CookieThemeResolver();
	}

}
