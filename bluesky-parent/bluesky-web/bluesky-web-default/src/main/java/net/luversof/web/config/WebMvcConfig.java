package net.luversof.web.config;

import java.util.List;

import net.luversof.web.blog.support.BlogHandlerMethodArgumentResolver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;


@Configuration
@PropertySource("web.properties")
public class WebMvcConfig extends WebMvcConfigurerAdapter {

	@Autowired
	private BlogHandlerMethodArgumentResolver blogHandlerMethodArgumentResolver;

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
		argumentResolvers.add(blogHandlerMethodArgumentResolver);
		super.addArgumentResolvers(argumentResolvers);
	}
}