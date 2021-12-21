package net.luversof.bookkeeping.config;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import net.luversof.bookkeeping.util.BookkeepingUtils;

@Configuration
@PropertySource("classpath:bookkeeping.properties")
public class BookeepingConfig implements ApplicationContextAware {
	
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		BookkeepingUtils.setApplicationContext(applicationContext);
	}

}
