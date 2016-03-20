package net.luversof.web.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.MediaType;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.accept.PathExtensionContentNegotiationStrategy;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;
import org.thymeleaf.spring4.view.ThymeleafViewResolver;

import net.luversof.web.blog.method.support.BlogHandlerMethodArgumentResolver;
import net.luversof.web.bookkeeping.method.support.BookkeepingHandlerMethodArgumentResolver;


@Configuration
@PropertySource("classpath:web.properties")
@PropertySource("classpath:web-${spring.profiles.active}.properties")
public class WebMvcConfig extends WebMvcConfigurerAdapter {

	@Autowired
	private BlogHandlerMethodArgumentResolver blogHandlerMethodArgumentResolver;
	
	@Autowired
	private BookkeepingHandlerMethodArgumentResolver bookkeepingHandlerMethodArgumentResolver;
	
	@Autowired
	private ThymeleafViewResolver thymeleafViewResolver;

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
		argumentResolvers.add(blogHandlerMethodArgumentResolver);
		argumentResolvers.add(bookkeepingHandlerMethodArgumentResolver);
		super.addArgumentResolvers(argumentResolvers);
	}
	
//	@Bean
//	public Java8TimeDialect java8TimeDialect() {
//		return new Java8TimeDialect();
//	}

	@Override
	public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
		configurer.favorPathExtension(true)
			.defaultContentType(MediaType.APPLICATION_JSON)
			.mediaType("html", MediaType.TEXT_HTML)
			.mediaType("json", MediaType.APPLICATION_JSON);
	}
	
	@Override
	public void configureViewResolvers(ViewResolverRegistry registry) {
		registry.viewResolver(thymeleafViewResolver);
		registry.enableContentNegotiation(new MappingJackson2JsonView());
	}
	
	
	

//	@Bean
//	public ContentNegotiatingViewResolver viewResolver() {
//		ContentNegotiatingViewResolver viewResolver = new ContentNegotiatingViewResolver();
//		
//		Map<String, MediaType> mediaTypes = new HashMap<>();
//		mediaTypes.put("html", MediaType.TEXT_HTML);
//		mediaTypes.put("json", MediaType.APPLICATION_JSON);
//		ContentNegotiationStrategy contentNegotiationStrategy = new PathExtensionContentNegotiationStrategy(mediaTypes);
//		
//		viewResolver.setContentNegotiationManager(new ContentNegotiationManager(contentNegotiationStrategy));
//
//		List<ViewResolver> viewResolvers = new ArrayList<>();
//		viewResolvers.add(thymeleafViewResolver);
//		viewResolver.setViewResolvers(viewResolvers);
//
//		List<View> defaultViews = new ArrayList<>();
//		MappingJackson2JsonView mappingJackson2JsonView = new MappingJackson2JsonView();
//		defaultViews.add(mappingJackson2JsonView);
//
//		viewResolver.setDefaultViews(defaultViews);
//		return viewResolver;
//	}
}