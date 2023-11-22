package net.luversof.web.gate.mustache.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import net.luversof.web.gate.mustache.interceptor.MustachePagedListHolderInterceptor;

@Configuration
public class GateMustacheWebMvcConfig implements WebMvcConfigurer {

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
//		registry.addInterceptor(new MustachePagedListHolderInterceptor());
		registry.addWebRequestInterceptor(new MustachePagedListHolderInterceptor());
	}
	
}
