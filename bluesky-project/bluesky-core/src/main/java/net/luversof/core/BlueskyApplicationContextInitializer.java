package net.luversof.core;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public class BlueskyApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		Banner.write(System.out, "BlueskyApplicationContextInitializer");
		applicationContext.getEnvironment().getConversionService().addConverter(new BlueskyEncryptConverter());
		
	}

}
