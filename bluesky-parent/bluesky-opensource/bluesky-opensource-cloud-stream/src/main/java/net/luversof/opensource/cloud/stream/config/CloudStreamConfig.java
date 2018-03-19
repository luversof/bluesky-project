package net.luversof.opensource.cloud.stream.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:cloud-stream-${spring.profiles.active}.properties")
public class CloudStreamConfig {

}
