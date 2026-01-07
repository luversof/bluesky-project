package net.luversof.web.common.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.PropertySource;

@AutoConfiguration("blueskyWebCommonAutoConfiguration")
@EnableConfigurationProperties(WebCommonProperties.class)
@PropertySource(value = "classpath:bluesky-web-common.properties", ignoreResourceNotFound = true)
public class WebCommonAutoConfiguration {

}
