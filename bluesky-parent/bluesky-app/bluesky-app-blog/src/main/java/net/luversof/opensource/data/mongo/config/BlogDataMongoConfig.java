package net.luversof.opensource.data.mongo.config;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import com.mongodb.client.MongoClient;

import net.luversof.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import net.luversof.boot.autoconfigure.mongo.config.MongoProperties;

@Configuration
@EnableMongoRepositories(basePackages = "net.luversof.blog.repository.mongo", mongoTemplateRef = "blogMongoTemplate")
@PropertySource(value = "classpath:blog-data-mongodb-${spring.profiles.active}.properties", ignoreResourceNotFound = true)
//@EnableMongoAuditing
public class BlogDataMongoConfig {

//	@Bean
//	public MongoTemplate blogMongoTemplate(@Qualifier("blogMongoDatabaseFactory") MongoDatabaseFactory blogMongoDatabaseFactory) {
//		return new MongoTemplate(blogMongoDatabaseFactory);
//	}
	
//	@Autowired
//	private MongoProperties mongoProperties;
//	
//	@Autowired
//	private MongoTemplate blogMongoTemplate
	
//	@Bean
//	public MongoTemplate blogMongoTemplate(/* @Qualifier("blogMongoClient") */ MongoClient mongoClient) {
//		return new MongoTemplate(mongoClient, "blog");
//	}
}
