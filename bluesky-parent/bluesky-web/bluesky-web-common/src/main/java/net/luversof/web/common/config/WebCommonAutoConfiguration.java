package net.luversof.web.common.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import net.luversof.web.common.controller.WebCommonDevCheckController;

@AutoConfiguration("blueskyWebCommonAutoConfiguration")
@EnableConfigurationProperties(WebCommonProperties.class)
public class WebCommonAutoConfiguration {

	@AutoConfiguration
	@ConditionalOnClass(name = "io.github.luversof.boot.devcheck.annotation.DevCheckController")
	static class DevCheckControllerConfiguration {

		@Bean
		WebCommonDevCheckController webCommonDevCheckController() {
			return new WebCommonDevCheckController();
		}
	
	}
	
}
