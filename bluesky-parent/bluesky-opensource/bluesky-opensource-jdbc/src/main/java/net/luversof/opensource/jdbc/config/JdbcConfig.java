package net.luversof.opensource.jdbc.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:jdbc.properties")
@PropertySource("classpath:jdbc-${spring.profiles.active}.properties")
public class JdbcConfig {

}
