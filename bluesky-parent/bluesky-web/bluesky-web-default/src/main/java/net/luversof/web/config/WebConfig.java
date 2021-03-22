package net.luversof.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ui.context.ThemeSource;
import org.springframework.ui.context.support.ResourceBundleThemeSource;

@Configuration
public class WebConfig {
	
	@Bean
	public ThemeSource themeSource() {
		ResourceBundleThemeSource resourceBundleThemeSource = new ResourceBundleThemeSource();
		resourceBundleThemeSource.setBasenamePrefix("static/theme/");
		return resourceBundleThemeSource;
	}

// boot 2.4 이후 이거 bean 정의 사용이 안되는데 확인 필요
//	@Bean
//	public ThemeResolver themeResolver() {
//		CookieThemeResolver cookieThemeResolver = new CookieThemeResolver();
//		cookieThemeResolver.setDefaultThemeName("default");
//		return cookieThemeResolver;
//	}

}
