package net.luversof.blog.config;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import net.luversof.blog.util.BlogRequestAttributeUtil;

@Configuration
@PropertySource("classpath:blog.properties")
public class BlogConfig implements ApplicationContextAware {

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		BlogRequestAttributeUtil.setApplicationContext(applicationContext);
	}

}
