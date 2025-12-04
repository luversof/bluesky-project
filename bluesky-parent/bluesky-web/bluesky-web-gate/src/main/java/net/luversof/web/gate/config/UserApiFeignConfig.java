//package net.luversof.web.gate.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.MediaType;
//
//import feign.RequestInterceptor;
//
///**
// * UserApiClient 전용 Feign 설정
// * OAuth2 토큰 인터셉터를 적용하지 않음 (토큰 저장 시점에는 인증이 완료되지 않았기 때문)
// */
//public class UserApiFeignConfig {
//
//	@Bean
//	RequestInterceptor userApiRequestInterceptor() {
//		return requestTemplate -> {
//			requestTemplate.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
//			// OAuth2 토큰 전파 없음 - User API는 내부 서비스 간 통신용
//		};
//	}
//}
