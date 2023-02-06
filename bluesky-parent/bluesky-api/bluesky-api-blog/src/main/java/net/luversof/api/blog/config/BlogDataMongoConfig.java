package net.luversof.api.blog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "net.luversof.api.blog.repository.mongo", mongoTemplateRef = "blogMongoTemplate")
//@EnableMongoAuditing
public class BlogDataMongoConfig {
}
