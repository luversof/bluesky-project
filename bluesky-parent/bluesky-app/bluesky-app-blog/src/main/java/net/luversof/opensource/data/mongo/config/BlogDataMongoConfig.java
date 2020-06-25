package net.luversof.opensource.data.mongo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "net.luversof.blog.repository.mongo", mongoTemplateRef = "blogMongoTemplate")
@PropertySource(value = "classpath:blog-data-mongodb-${spring.profiles.active}.properties", ignoreResourceNotFound = true)
//@EnableMongoAuditing
public class BlogDataMongoConfig {
}
