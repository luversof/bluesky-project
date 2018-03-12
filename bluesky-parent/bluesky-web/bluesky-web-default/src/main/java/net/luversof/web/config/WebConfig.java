package net.luversof.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.ThemeResolver;
import org.springframework.web.servlet.theme.CookieThemeResolver;

@Configuration
public class WebConfig {

	/**
	 * spring의 ThemeResolver는 모든 properties를 불러올수 있으므로 실제 사용시엔 제약사항을 두는게 좋을 듯
	 * @return
	 */
	@Bean
	public ThemeResolver themeResolver() {
		CookieThemeResolver cookieThemeResolver = new CookieThemeResolver();
		cookieThemeResolver.setDefaultThemeName("theme-default");
		return cookieThemeResolver;
	}

}
