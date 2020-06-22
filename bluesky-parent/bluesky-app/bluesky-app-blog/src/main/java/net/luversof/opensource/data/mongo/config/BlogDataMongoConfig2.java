package net.luversof.opensource.data.mongo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import com.mongodb.client.MongoClients;

@Configuration
@EnableMongoRepositories(basePackages = "net.luversof.blog.repository.mongo2", mongoTemplateRef = "blogMongoTemplate2")
//@EnableMongoAuditing
public class BlogDataMongoConfig2 {

	@Bean
	public MongoTemplate blogMongoTemplate2() {
		MongoDatabaseFactory factory = new SimpleMongoClientDatabaseFactory(MongoClients.create("mongodb://localhost"), "bookkeeping");
		return new MongoTemplate(factory);
	}
}
