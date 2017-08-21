package net.luversof.web.config;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
//@EnableDiscoveryClient
public class WebMvcConfig implements WebMvcConfigurer {

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