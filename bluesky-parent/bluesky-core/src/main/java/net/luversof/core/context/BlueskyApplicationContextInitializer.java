package net.luversof.core.context;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import net.luversof.core.converter.EncryptStringConverter;

public class BlueskyApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		applicationContext.getEnvironment().getConversionService().addConverter(new EncryptStringConverter());
	}

}
