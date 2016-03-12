package net.luversof.opensource.data.jpa.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
@PropertySource("classpath:data-jpa.properties")
@PropertySource("classpath:data-jpa-${spring.profiles.active}.properties")
public class DataJpaConfig {

}
