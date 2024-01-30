package net.luversof.web.dynamiccrud.config;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.mongodb.client.MongoClient;

import io.github.luversof.boot.connectioninfo.ConnectionInfoCollector;
import io.github.luversof.boot.security.crypto.env.DecryptPropertySourceFactory;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@PropertySource(value = "classpath:mongo-${bluesky-boot-profile}.properties", factory = DecryptPropertySourceFactory.class, ignoreResourceNotFound = true)
public class DynamicCrudMongoConfig {

	@Autowired(required = false)
	private Map<String, ConnectionInfoCollector<MongoClient>> mongoClientConnectionInfoCollectorMap;
	
	@PostConstruct
	public void postConstruct() {
		log.debug("Test : {}", mongoClientConnectionInfoCollectorMap);
	}
}
