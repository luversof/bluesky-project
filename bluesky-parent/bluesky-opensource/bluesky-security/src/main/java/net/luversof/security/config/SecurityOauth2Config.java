package net.luversof.security.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationProcessingFilter;

@Configuration
@EnableOAuth2Client
public class SecurityOauth2Config {
	
	@Autowired
	private OAuth2ClientContext oAuth2ClientContext;
	
	@Bean
	public OAuth2ProtectedResourceDetails oAuth2ProtectedResourceDetails() {
		AuthorizationCodeResourceDetails details = new AuthorizationCodeResourceDetails();
		details.setId("github");
		details.setClientId("5ce0e9ac811fd9c04543");
		details.setClientSecret("b280853a74e6ae138ac23805092ddca670624ac9");
		details.setAccessTokenUri("https://github.com/login/oauth/access_token");
		details.setUserAuthorizationUri("https://github.com/login/oauth/authorize");
		details.setScope(Arrays.asList("user"));
		return details;
	}
	
	@Bean
	public OAuth2RestTemplate oAuth2RestTemplate() {
		return new OAuth2RestTemplate(oAuth2ProtectedResourceDetails(), oAuth2ClientContext);
	}
	
//	@Bean
	public OAuth2ClientAuthenticationProcessingFilter oAuth2ClientAuthenticationProcessingFilter() {
		OAuth2ClientAuthenticationProcessingFilter oAuth2ClientAuthenticationProcessingFilter = new OAuth2ClientAuthenticationProcessingFilter("/oauthLogin");
		oAuth2ClientAuthenticationProcessingFilter.setRestTemplate(oAuth2RestTemplate());
		return oAuth2ClientAuthenticationProcessingFilter;
	}
	
	
}
