package net.luversof.client.user.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;

import net.luversof.client.user.httpexchange.UserInfoApiClient;
import net.luversof.client.user.session.ApiSessionRepository;

@Configuration
@ConditionalOnMissingClass("org.springframework.session.data.redis.RedisSessionRepository")
@EnableSpringHttpSession
public class ApiSessionConfig {

  @Bean
  ApiSessionRepository sessionRepository(UserInfoApiClient userInfoApiClient) {
    return new ApiSessionRepository(userInfoApiClient);
  }
}
