package net.luversof.web.gate.config;

import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;

@Configuration
public class GateHttpExchangeConfig {

  @Bean
  public OAuth2AuthorizedClientManager authorizedClientManager(
      ClientRegistrationRepository clientRegistrationRepository,
      OAuth2AuthorizedClientRepository authorizedClientRepository) {

    OAuth2AuthorizedClientProvider authorizedClientProvider =
        OAuth2AuthorizedClientProviderBuilder.builder()
            .authorizationCode()
            .refreshToken()
            .clientCredentials()
            .build();

    DefaultOAuth2AuthorizedClientManager authorizedClientManager =
        new DefaultOAuth2AuthorizedClientManager(
            clientRegistrationRepository, authorizedClientRepository);
    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

    return authorizedClientManager;
  }

  @Bean
  RestClientCustomizer oauth2RestClientCustomizer(
      OAuth2AuthorizedClientManager authorizedClientManager) {
    return builder ->
        builder.requestInterceptor(
            (request, body, execution) -> {
              if (!request.getURI().getPath().startsWith("/api/userInfo")) {
                Authentication authentication =
                    SecurityContextHolder.getContext().getAuthentication();
                if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
                  try {
                    OAuth2AuthorizeRequest authorizeRequest =
                        OAuth2AuthorizeRequest.withClientRegistrationId(
                                oauthToken.getAuthorizedClientRegistrationId())
                            .principal(authentication)
                            .build();

                    OAuth2AuthorizedClient authorizedClient =
                        authorizedClientManager.authorize(authorizeRequest);
                    if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
                      request
                          .getHeaders()
                          .setBearerAuth(authorizedClient.getAccessToken().getTokenValue());
                    }
                  } catch (Exception e) {
                    // Ignore if authorization fails
                  }
                }
              }
              return execution.execute(request, body);
            });
  }
}
