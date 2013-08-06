package net.luversof.web.config.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import nz.net.ultraq.thymeleaf.LayoutDialect;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.springframework.web.accept.PathExtensionContentNegotiationStrategy;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;
import org.springframework.web.servlet.view.json.MappingJacksonJsonView;
import org.thymeleaf.extras.springsecurity3.dialect.SpringSecurityDialect;
import org.thymeleaf.spring3.SpringTemplateEngine;
import org.thymeleaf.spring3.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

@Slf4j
@Configuration
@EnableWebMvc
@ComponentScan(basePackages = "net.luversof",useDefaultFilters=false,includeFilters=@Filter(type=FilterType.ANNOTATION, value={ Controller.class, ControllerAdvice.class }))
//@PropertySource(name="mvcProps", value="classpath:props/mvc.properties")
@EnableAspectJAutoProxy
public class WebMvcConfig extends WebMvcConfigurerAdapter {

	@Autowired
	private ApplicationContext applicationContext;
	

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		registry.addViewController("/").setViewName("index");
		registry.addViewController("/loginPage").setViewName("/loginPage");
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/resources/**").addResourceLocations("/public-resources/");
		registry.addResourceHandler("/css/**").addResourceLocations("/public-resources/css/");
		registry.addResourceHandler("/js/**").addResourceLocations("/public-resources/js/");
		registry.addResourceHandler("/favicon.ico").addResourceLocations("/public-resources/img/favicon.ico").setCachePeriod(31556926);
		registry.addResourceHandler("/img/**").addResourceLocations("/public-resources/img/").setCachePeriod(31556926);
	}

	@Override
	public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
		configurer.enable();
	}


//	@Bean
//	public LoginCheckInterceptor loginCheckInterceptor() {
//		return new LoginCheckInterceptor();
//	}
//	
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new LocaleChangeInterceptor());

		super.addInterceptors(registry);
	}

	@Bean
	public SessionLocaleResolver localeResolver() {
		SessionLocaleResolver sessionLocaleResolver = new SessionLocaleResolver();
		sessionLocaleResolver.setDefaultLocale(Locale.KOREA);
		return sessionLocaleResolver;
	}

	@Bean
	public LocaleChangeInterceptor localeChangeInterceptor() {
		return new LocaleChangeInterceptor();
	}

//	@Bean
//	public SimpleMappingExceptionResolver simpleMappingExceptionResolver () {
//		SimpleMappingExceptionResolver simpleMappingExceptionResolver = new SimpleMappingExceptionResolver();
//		simpleMappingExceptionResolver.setDefaultErrorView("error/error");
//		Properties mappings = new Properties();
//		mappings.put("net.luversof.core.exception.GenericException", "error/error");
//		mappings.put("net.luversof.core.exception.UnauthorizedException", "error/error");
//		simpleMappingExceptionResolver.setExceptionMappings(mappings);
//
//		return simpleMappingExceptionResolver;
//	}


	@Bean
	public ContentNegotiatingViewResolver contentNegotiatingViewResolver() {
		ContentNegotiatingViewResolver viewResolver = new ContentNegotiatingViewResolver();

		// 확장자 기준 결정
		Map<String, MediaType> mediaTypes = new HashMap<String, MediaType>();
		mediaTypes.put("html", MediaType.TEXT_HTML);
		mediaTypes.put("json", MediaType.APPLICATION_JSON);
		ContentNegotiationStrategy pathExtensionContentNegotiationStrategy = new PathExtensionContentNegotiationStrategy(mediaTypes);

		// 헤더값 기준 결정
		ContentNegotiationStrategy  headerContentNegotiationStrategy = new HeaderContentNegotiationStrategy();

		/* order에 따라 우선순위 결정되므로 순서 주의 */
		ContentNegotiationManager contentNegotiationManager = new ContentNegotiationManager(pathExtensionContentNegotiationStrategy, headerContentNegotiationStrategy);
		viewResolver.setContentNegotiationManager(contentNegotiationManager);


		 /*contentNegotiationManagerFactoryBean의 mediaType를 사용한 방법*/

		// ContentNegotiationManagerFactoryBean
		// contentNegotiationManagerFactoryBean = new
		// ContentNegotiationManagerFactoryBean();
		// Properties mediaTypes2 = new Properties();
		// mediaTypes2.put("html", "text/html");
		// mediaTypes2.put("json", "application/json");
		// contentNegotiationManagerFactoryBean.setMediaTypes(mediaTypes2);
		// try {
		// viewResolver.setContentNegotiationManager(contentNegotiationManagerFactoryBean.getObject());
		// } catch (Exception e) {
		// e.printStackTrace();
		// }

		List<ViewResolver> viewResolvers = new ArrayList<ViewResolver>();

		ThymeleafViewResolver thymeleafViewResolver = new ThymeleafViewResolver();
		thymeleafViewResolver.setTemplateEngine(templateEngine());
		thymeleafViewResolver.setCharacterEncoding("UTF-8");

		viewResolvers.add(thymeleafViewResolver);
		viewResolver.setViewResolvers(viewResolvers);


		List<View> defaultViews = new ArrayList<View>();
		defaultViews.add(new MappingJacksonJsonView());

		viewResolver.setDefaultViews(defaultViews);
		return viewResolver;
	}

	@Bean
	public ServletContextTemplateResolver templateResolver() {
		ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver();
		templateResolver.setPrefix("/WEB-INF/views/");
		templateResolver.setSuffix(".html");
		templateResolver.setTemplateMode("HTML5");
		// templateResolver.setCharacterEncoding("UTF-8");
		// 개발시엔 false로 설정하자.

		if (ArrayUtils.contains(applicationContext.getEnvironment().getActiveProfiles(), "live")) {
			templateResolver.setCacheable(true);
		} else {
			templateResolver.setCacheable(false);
		}
		return templateResolver;
	}

	@Bean
	public SpringTemplateEngine templateEngine() {
		SpringTemplateEngine springTemplateEngine = new SpringTemplateEngine();
		springTemplateEngine.setTemplateResolver(templateResolver());
		springTemplateEngine.setMessageSource(messageSource());
		springTemplateEngine.addDialect(new LayoutDialect());	// thymeleaf-layout-dialect 사용 설정
		springTemplateEngine.addDialect(new SpringSecurityDialect());	//thymeleaf-extras-springsecurity3 사용 설정
		return springTemplateEngine;
	}

	@Bean
	public MessageSource messageSource() {
		log.debug("setting up message source");
		ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
		messageSource.setBasename("public-resources/message/message");
		messageSource.setCacheSeconds(5);
		messageSource.setDefaultEncoding("UTF-8");
		return messageSource;
	}

	// @Bean
	// public ThymeleafViewResolver thymeleafViewResolver() {
	// ThymeleafViewResolver thymeleafViewResolver = new
	// ThymeleafViewResolver();
	// thymeleafViewResolver.setTemplateEngine(templateEngine());
	// thymeleafViewResolver.setCharacterEncoding("UTF-8");
	// // thymeleafViewResolver.setCache(false);
	// return thymeleafViewResolver;
	// }

}