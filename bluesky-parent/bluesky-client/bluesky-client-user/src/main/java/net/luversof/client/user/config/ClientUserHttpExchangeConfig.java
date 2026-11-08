package net.luversof.client.user.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import net.luversof.client.user.httpexchange.UserInfoApiClient;

@Configuration
public class ClientUserHttpExchangeConfig {

  /** Same name as ResourceServerConfig.INTERNAL_TOKEN_HEADER in bluesky-api-user. */
  static final String INTERNAL_TOKEN_HEADER = "X-Bluesky-Internal-Token";

  /**
   * Built here, not with the shared httpServiceProxyFactoryBuilder, so the internal token goes only
   * to bluesky-api-user and never to the other APIs.
   */
  @Bean
  @ConditionalOnMissingBean(name = "clientUserHttpServiceProxyFactory")
  HttpServiceProxyFactory clientUserHttpServiceProxyFactory(
      @LoadBalanced ObjectProvider<RestClient.Builder> loadBalancedRestClientBuilderProvider,
      @Qualifier("restClientBuilder") ObjectProvider<RestClient.Builder> restClientBuilderProvider,
      @Value("${spring.http.serviceclient.client-user.base-url}") String baseUrl,
      @Value("${bluesky.internal-api.token:}") String internalToken) {
    RestClient.Builder builder =
        baseUrl.startsWith("lb://")
            ? loadBalancedRestClientBuilderProvider.getObject()
            : restClientBuilderProvider.getObject();
    RestClient restClient =
        builder.baseUrl(baseUrl).defaultHeader(INTERNAL_TOKEN_HEADER, internalToken).build();
    return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient)).build();
  }

  @Bean
  UserInfoApiClient userInfoApiClient(
      @Qualifier("clientUserHttpServiceProxyFactory")
          HttpServiceProxyFactory clientUserHttpServiceProxyFactory) {
    return clientUserHttpServiceProxyFactory.createClient(UserInfoApiClient.class);
  }
}
