package net.luversof.web.common.menu.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@AutoConfiguration("blueskyWebCommonAutoConfiguration")
@EnableConfigurationProperties(WebCommonProperties.class)
public class WebCommonAutoConfiguration {

}
