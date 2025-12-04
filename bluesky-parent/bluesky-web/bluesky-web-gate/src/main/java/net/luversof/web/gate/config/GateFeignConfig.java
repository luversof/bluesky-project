//package net.luversof.web.gate.config;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.context.properties.EnableConfigurationProperties;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.MediaType;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
//import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
//import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
//
//import feign.Feign;
//import feign.Logger.Level;
//import feign.RequestInterceptor;
//import io.micrometer.core.instrument.MeterRegistry;
//import lombok.Setter;
//
//@Configuration
//@EnableConfigurationProperties(GateFeignProperties.class)
//public class GateFeignConfig {
//
//	@Setter(onMethod_ = @Autowired)
//	private OAuth2AuthorizedClientManager authorizedClientManager;
//
//	/*
//	 * feign client 전체 적용
//	 */
//	@Bean
//	RequestInterceptor feignClientRequestInterceptor() {
//		return requestTemplate -> {
//			requestTemplate.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
//
//			// OAuth2 Token 자동 전파
//			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//
//			if (authentication != null && authentication.isAuthenticated() && authorizedClientManager != null) {
//				try {
//					OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
//							.withClientRegistrationId("bluesky")
//							.principal(authentication)
//							.build();
//
//					OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);
//
//					if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
//						String accessToken = authorizedClient.getAccessToken().getTokenValue();
//						requestTemplate.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
//					}
//				} catch (Exception e) {
//					// Token 획득 실패 시 무시 (로그인하지 않은 요청)
//				}
//			}
//		};
//	}
//
//	@Bean
//	Feign.Builder feignBuilder(MeterRegistry meterRegistry) {
//		return Feign
//				.builder()
//				.logLevel(Level.FULL);
//	}
//}
