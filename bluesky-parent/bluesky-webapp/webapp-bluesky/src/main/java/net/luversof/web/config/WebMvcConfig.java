package net.luversof.web.config;

//import static net.luversof.core.Constants.JSON_MODEL_KEY;
//
//import java.nio.charset.StandardCharsets;
//import java.text.SimpleDateFormat;
//import java.util.List;
//import java.util.Locale;
//
//import net.luversof.web.blog.support.BlogHandlerMethodArgumentResolver;
//import nz.net.ultraq.thymeleaf.LayoutDialect;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.ApplicationContext;
//import org.springframework.context.MessageSource;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.method.support.HandlerMethodArgumentResolver;
//import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
//import org.springframework.web.servlet.config.annotation.EnableWebMvc;
//import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
//import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
//import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
//import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
//import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
//import org.springframework.web.servlet.i18n.SessionLocaleResolver;
//import org.springframework.web.servlet.view.json.MappingJackson2JsonView;
//import org.thymeleaf.extras.springsecurity3.dialect.SpringSecurityDialect;
//import org.thymeleaf.spring4.SpringTemplateEngine;
//import org.thymeleaf.spring4.view.ThymeleafViewResolver;
//import org.thymeleaf.templateresolver.ServletContextTemplateResolver;
//import org.thymeleaf.util.ArrayUtils;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//
//
//@Configuration
//@EnableWebMvc
//public class WebMvcConfig extends WebMvcConfigurerAdapter {
//
//	private static final int RESOURCE_CACHE_PERIOD = 31556926;
//
//	@Autowired
//	private ApplicationContext applicationContext;
//	
//	@Autowired
//	private BlogHandlerMethodArgumentResolver blogHandlerMethodArgumentResolver;
//	
//	@Autowired
//	private MessageSource messageSource;
//
//	@Override
//	public void addViewControllers(ViewControllerRegistry registry) {
//		registry.addViewController("/").setViewName("/index");
//		registry.addViewController("/login").setViewName("/login");
//	}
//
//	@Override
//	public void addResourceHandlers(ResourceHandlerRegistry registry) {
//		registry.addResourceHandler("/resources/**").addResourceLocations("/public-resources/");
//		registry.addResourceHandler("/css/**").addResourceLocations("/public-resources/css/");
//		registry.addResourceHandler("/js/**").addResourceLocations("/public-resources/js/");
//		registry.addResourceHandler("/favicon.ico").addResourceLocations("/public-resources/img/favicon.ico").setCachePeriod(RESOURCE_CACHE_PERIOD);
//		registry.addResourceHandler("/img/**").addResourceLocations("/public-resources/img/").setCachePeriod(RESOURCE_CACHE_PERIOD);
//	}
//
//	@Override
//	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
//		argumentResolvers.add(blogHandlerMethodArgumentResolver);
//		super.addArgumentResolvers(argumentResolvers);
//	}
//
//	@Override
//	public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
//		configurer.enable();
//	}
//
//	@Override
//	public void addInterceptors(InterceptorRegistry registry) {
//		registry.addInterceptor(new LocaleChangeInterceptor());
//	}
//
//	@Bean
//	public SessionLocaleResolver localeResolver() {
//		SessionLocaleResolver sessionLocaleResolver = new SessionLocaleResolver();
//		sessionLocaleResolver.setDefaultLocale(Locale.KOREA);
//		return sessionLocaleResolver;
//	}
//	
//	/**
//	 * spring 4.1.0 이후 변경된 설정 방법 적용
//	 */
//	@Override
//	public void configureViewResolvers(ViewResolverRegistry registry) {
//		registry.viewResolver(thymeleafViewResolver());
//		
//		MappingJackson2JsonView mappingJackson2JsonView = new MappingJackson2JsonView();
//		mappingJackson2JsonView.setExtractValueFromSingleKeyModel(true);
//		mappingJackson2JsonView.setModelKey(JSON_MODEL_KEY);
//		ObjectMapper objectMapper = mappingJackson2JsonView.getObjectMapper();
//		//objectMapper.registerModule(new JSR310Module());
//		objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
//		registry.enableContentNegotiation(mappingJackson2JsonView);
//	}
//	
//	@Bean
//	public ThymeleafViewResolver thymeleafViewResolver() {
//		ThymeleafViewResolver thymeleafViewResolver = new ThymeleafViewResolver();
//		thymeleafViewResolver.setTemplateEngine(templateEngine());
//		thymeleafViewResolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
//		return thymeleafViewResolver;
//	}
//	
//	@Bean
//	public SpringTemplateEngine templateEngine() {
//		SpringTemplateEngine springTemplateEngine = new SpringTemplateEngine();
//		springTemplateEngine.setTemplateResolver(templateResolver());
//		springTemplateEngine.setMessageSource(messageSource);
//		// thymeleaf-layout-dialect 사용 설정
//		springTemplateEngine.addDialect(new LayoutDialect());
//		// thymeleaf-extras-springsecurity3 사용 설정
//		springTemplateEngine.addDialect(new SpringSecurityDialect());
//		return springTemplateEngine;
//	}
//
//	@Bean
//	public ServletContextTemplateResolver templateResolver() {
//		ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver();
//		templateResolver.setPrefix("/WEB-INF/views/");
//		templateResolver.setSuffix(".html");
//		templateResolver.setTemplateMode("HTML5");
//		templateResolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
//		templateResolver.setCacheable(ArrayUtils.contains(applicationContext.getEnvironment().getActiveProfiles(), "live"));
//		return templateResolver;
//	}
//}