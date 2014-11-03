package net.luversof.security.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.common.AuthenticationScheme;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationManager;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationProcessingFilter;
import org.springframework.security.oauth2.provider.code.AuthorizationCodeServices;
import org.springframework.security.oauth2.provider.code.InMemoryAuthorizationCodeServices;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.store.InMemoryTokenStore;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableOAuth2Client
public class OAuth2ClientConfig {
	
	@Autowired
	private OAuth2ClientContext oAuth2ClientContext;
	
//	@Autowired
//	private AccessTokenRequest accessTokenRequest;
	
	@Bean
	public OAuth2ProtectedResourceDetails oAuth2ProtectedResourceDetails() {
		AuthorizationCodeResourceDetails details = new AuthorizationCodeResourceDetails();
		details.setId("github");
		details.setClientId("5ce0e9ac811fd9c04543");
		details.setClientSecret("b280853a74e6ae138ac23805092ddca670624ac9");
		details.setAccessTokenUri("https://github.com/login/oauth/access_token");
		details.setUserAuthorizationUri("https://github.com/login/oauth/authorize");
		details.setScope(Arrays.asList("read"));
		details.setAuthenticationScheme(AuthenticationScheme.query);
		details.setTokenName("access_token");
		details.setGrantType("authorization_code");
		details.setClientAuthenticationScheme(AuthenticationScheme.form);
		return details;
	}
	
	@Bean
//	@Scope(value = "session", proxyMode = ScopedProxyMode.INTERFACES)
	public OAuth2RestTemplate oAuth2RestTemplate() {
		return new OAuth2RestTemplate(oAuth2ProtectedResourceDetails(), oAuth2ClientContext);
//		return new OAuth2RestTemplate(oAuth2ProtectedResourceDetails(), new DefaultOAuth2ClientContext(accessTokenRequest));
	}
	
//	@Bean
	public OAuth2ClientAuthenticationProcessingFilter oAuth2ClientAuthenticationProcessingFilter() {
		OAuth2ClientAuthenticationProcessingFilter oAuth2ClientAuthenticationProcessingFilter = new OAuth2ClientAuthenticationProcessingFilter("/oauthLogin");
		oAuth2ClientAuthenticationProcessingFilter.setRestTemplate(oAuth2RestTemplate());
		oAuth2ClientAuthenticationProcessingFilter.setRequiresAuthenticationRequestMatcher(new AntPathRequestMatcher("/oauth/authorize"));
		return oAuth2ClientAuthenticationProcessingFilter;
	}
	
	
//	@Bean
	public DefaultTokenServices defaultTokenServices() {
		DefaultTokenServices defaultTokenServices = new DefaultTokenServices();
		defaultTokenServices.setTokenStore(new InMemoryTokenStore());
		return defaultTokenServices;
	}
	
//	@Bean
	public OAuth2AuthenticationManager OAuth2AuthenticationManager() {
		OAuth2AuthenticationManager oAuth2AuthenticationManager = new OAuth2AuthenticationManager();
		oAuth2AuthenticationManager.setTokenServices(defaultTokenServices());
		return oAuth2AuthenticationManager;
	}
	
//	@Bean
	public OAuth2AuthenticationProcessingFilter oAuth2AuthenticationProcessingFilter() {
		OAuth2AuthenticationProcessingFilter oAuth2AuthenticationProcessingFilter = new OAuth2AuthenticationProcessingFilter();
		oAuth2AuthenticationProcessingFilter.setAuthenticationManager(OAuth2AuthenticationManager());
		return oAuth2AuthenticationProcessingFilter;
	}
}
