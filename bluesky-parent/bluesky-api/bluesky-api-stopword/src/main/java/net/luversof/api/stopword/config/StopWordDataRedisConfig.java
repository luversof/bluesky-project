package net.luversof.api.stopword.config;

import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@EnableRedisRepositories(basePackages = "net.luversof.api.stopword.**.repository" )
public class StopWordDataRedisConfig {

}
