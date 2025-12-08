//package net.luversof.web.gate.config;
//
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.service.registry.ImportHttpServices;
//
//import net.luversof.web.gate.blog.httpexchange.BlogArticleCategoryClient;
//import net.luversof.web.gate.blog.httpexchange.BlogArticleClient;
//import net.luversof.web.gate.blog.httpexchange.BlogArticleCommentClient;
//import net.luversof.web.gate.blog.httpexchange.BlogClient;
//
//@Configuration
//public class GateHttpExchangeConfig {
//	
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
//}
