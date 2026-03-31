package net.luversof.api.user.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@Configuration
@EnableRedisHttpSession(
        redisNamespace = "${spring.session.redis.namespace:spring:session}",
        maxInactiveIntervalInSeconds = 14400)
public class RedisSessionConfig {}
