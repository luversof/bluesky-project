package net.luversof.web.user.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import net.luversof.web.user.interceptor.LoginRedirectUrlInterceptor;

@Configuration
public class WebUserWebMvcConfig implements WebMvcConfigurer {

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new LoginRedirectUrlInterceptor()).addPathPatterns("/login");
	}

}
