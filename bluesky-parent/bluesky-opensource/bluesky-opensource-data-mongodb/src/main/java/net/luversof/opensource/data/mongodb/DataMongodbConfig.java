package net.luversof.opensource.data.mongodb;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:data-mongodb-${spring.profiles.active}.properties")
public class DataMongodbConfig {

}
