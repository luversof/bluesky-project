package net.luversof.web.config;

import java.nio.charset.StandardCharsets;

import javax.servlet.Filter;

import net.luversof.blog.config.BlogConfig;
import net.luversof.bookkeeping.config.BookkeepingConfig;
import net.luversof.data.jpa.config.JpaConfig;
import net.luversof.security.config.SecurityConfig;

import org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.HiddenHttpMethodFilter;
import org.springframework.web.filter.HttpPutFormContentFilter;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

public class DispatcherServletInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

	@Override
	protected Class<?>[] getRootConfigClasses() {
		return new Class[] { JpaConfig.class, BlogConfig.class, BookkeepingConfig.class, SecurityConfig.class};
	}

	@Override
	protected Class<?>[] getServletConfigClasses() {
		return new Class[] { WebMvcConfig.class };
	}

	@Override
	protected String[] getServletMappings() {
		return new String[] { "/" };
	}
	
	@Override
	protected Filter[] getServletFilters() {
		CharacterEncodingFilter characterEncodingFilter = new CharacterEncodingFilter();
		characterEncodingFilter.setEncoding(StandardCharsets.UTF_8.name());
		characterEncodingFilter.setForceEncoding(true);
		return new Filter[] { characterEncodingFilter, new HiddenHttpMethodFilter(), new HttpPutFormContentFilter(), new OpenEntityManagerInViewFilter()};
	}
}