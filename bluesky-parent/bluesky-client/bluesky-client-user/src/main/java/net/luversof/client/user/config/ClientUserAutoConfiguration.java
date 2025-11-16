package net.luversof.client.user.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ClientUserFeignProperties.class)
public class ClientUserAutoConfiguration {

}
