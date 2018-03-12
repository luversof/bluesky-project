package net.luversof.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ui.context.ThemeSource;
import org.springframework.ui.context.support.ResourceBundleThemeSource;
import org.springframework.web.servlet.ThemeResolver;
import org.springframework.web.servlet.theme.CookieThemeResolver;

@Configuration
public class WebConfig {
	
	@Bean
	public ThemeSource themeSource() {
		ResourceBundleThemeSource resourceBundleThemeSource = new ResourceBundleThemeSource();
		resourceBundleThemeSource.setBasenamePrefix("static/theme/");
		return resourceBundleThemeSource;
	}

	@Bean
	public ThemeResolver themeResolver() {
		CookieThemeResolver cookieThemeResolver = new CookieThemeResolver();
		cookieThemeResolver.setDefaultThemeName("default");
		return cookieThemeResolver;
	}

}
