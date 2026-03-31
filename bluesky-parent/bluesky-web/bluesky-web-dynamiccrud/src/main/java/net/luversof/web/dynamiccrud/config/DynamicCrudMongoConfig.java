package net.luversof.web.dynamiccrud.config;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.mongodb.client.MongoClient;

import io.github.luversof.boot.connectioninfo.ConnectionInfoRegistry;
import io.github.luversof.boot.security.crypto.support.DecryptPropertySourceFactory;
import jakarta.annotation.PostConstruct;

@Configuration
@PropertySource(
        value = "classpath:mongo-${bluesky-boot-profile}.properties",
        factory = DecryptPropertySourceFactory.class,
        ignoreResourceNotFound = true)
public class DynamicCrudMongoConfig {

    private static final Logger log = LoggerFactory.getLogger(DynamicCrudMongoConfig.class);

    @Autowired(required = false)
    private Map<String, ConnectionInfoRegistry<MongoClient>> mongoClientConnectionRegistry;

    @PostConstruct
    public void postConstruct() {
        log.debug("Test : {}", mongoClientConnectionRegistry);
    }
}
