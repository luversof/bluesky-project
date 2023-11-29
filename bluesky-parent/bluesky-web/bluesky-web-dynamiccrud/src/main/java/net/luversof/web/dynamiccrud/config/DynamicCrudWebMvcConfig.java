package net.luversof.web.dynamiccrud.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import net.luversof.web.dynamiccrud.interceptor.PaginationInterceptor;

@Configuration
public class DynamicCrudWebMvcConfig implements WebMvcConfigurer {

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addWebRequestInterceptor(new PaginationInterceptor());
	}

}
