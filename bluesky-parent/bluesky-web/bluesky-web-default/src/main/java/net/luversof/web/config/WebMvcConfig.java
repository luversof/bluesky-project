package net.luversof.web.config;

import java.util.List;

import net.luversof.web.blog.support.BlogHandlerMethodArgumentResolver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;


@Configuration
@PropertySource("classpath:web.properties")
@PropertySource("classpath:web-${spring.profiles.active}.properties")
public class WebMvcConfig extends WebMvcConfigurerAdapter {

	@Autowired
	private BlogHandlerMethodArgumentResolver blogHandlerMethodArgumentResolver;

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
		argumentResolvers.add(blogHandlerMethodArgumentResolver);
		super.addArgumentResolvers(argumentResolvers);
	}
	
	@Bean
	public Java8TimeDialect java8TimeDialect() {
		return new Java8TimeDialect();
	}
}