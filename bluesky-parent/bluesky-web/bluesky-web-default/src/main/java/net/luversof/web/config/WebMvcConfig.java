package net.luversof.web.config;

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.luversof.blog.web.method.support.UserBlogForBlogArticleHandlerMethodArgumentResolver;
import net.luversof.blog.web.method.support.UserBlogHandlerMethodArgumentResolver;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

//	@Autowired
//	private BlogHandlerMethodArgumentResolver blogHandlerMethodArgumentResolver;

	// @Autowired
	// private BookkeepingHandlerMethodArgumentResolver
	// bookkeepingHandlerMethodArgumentResolver;
	
	@Autowired
	private SpringResourceTemplateResolver defaultTemplateResolver;
	
	@PostConstruct
	public void postConstruct() {
		defaultTemplateResolver.setUseDecoupledLogic(true);
	}

	@Autowired
	private ThymeleafViewResolver thymeleafViewResolver;
	
	@Autowired
	private ObjectMapper objectMapper;

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
//		argumentResolvers.add(blogHandlerMethodArgumentResolver);
		// argumentResolvers.add(bookkeepingHandlerMethodArgumentResolver);
		argumentResolvers.add(new UserBlogHandlerMethodArgumentResolver());
		argumentResolvers.add(new UserBlogForBlogArticleHandlerMethodArgumentResolver());
	}

	// request date conversion 처리
	@Override
	public void addFormatters(FormatterRegistry registry) {
		DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
		registrar.setUseIsoFormat(true);
		registrar.registerFormatters(registry);
	}

	@Override
	public void configureViewResolvers(ViewResolverRegistry registry) {
		registry.viewResolver(thymeleafViewResolver);
		registry.enableContentNegotiation(new MappingJackson2JsonView(objectMapper));
	}
}