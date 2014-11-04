package net.luversof.security.config;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.common.AuthenticationScheme;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationManager;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationProcessingFilter;
import org.springframework.security.oauth2.provider.code.AuthorizationCodeServices;
import org.springframework.security.oauth2.provider.code.InMemoryAuthorizationCodeServices;
import org.springframework.security.oauth2.provider.token.AccessTokenConverter;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;
import org.springframework.security.oauth2.provider.token.store.InMemoryTokenStore;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

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
		details.setPreEstablishedRedirectUri("http://localhost:8081/oauth/authorizeResult");
		details.setScope(Arrays.asList("user"));
		details.setTokenName("access_token");
		details.setAuthenticationScheme(AuthenticationScheme.form);
		details.setClientAuthenticationScheme(AuthenticationScheme.form);
		return details;
	}
	
	@Bean
//	@Scope(value = "session", proxyMode = ScopedProxyMode.INTERFACES)
	public OAuth2RestTemplate oAuth2RestTemplate() {
		return new OAuth2RestTemplate(oAuth2ProtectedResourceDetails(), oAuth2ClientContext);
//		return new OAuth2RestTemplate(oAuth2ProtectedResourceDetails(), new DefaultOAuth2ClientContext(accessTokenRequest));
	}
	
	@Bean
	public OAuth2ClientAuthenticationProcessingFilter oAuth2ClientAuthenticationProcessingFilter() {
		OAuth2ClientAuthenticationProcessingFilter oAuth2ClientAuthenticationProcessingFilter = new OAuth2ClientAuthenticationProcessingFilter("/oauth/authorizeResult");
		oAuth2ClientAuthenticationProcessingFilter.setRestTemplate(oAuth2RestTemplate());
		oAuth2ClientAuthenticationProcessingFilter.setTokenServices(remoteTokenServices());
		return oAuth2ClientAuthenticationProcessingFilter;
	}
	
	
//	@Bean
//	public DefaultTokenServices defaultTokenServices() {
//		DefaultTokenServices defaultTokenServices = new DefaultTokenServices();
//		defaultTokenServices.setTokenStore(new InMemoryTokenStore());
//		defaultTokenServices.setSupportRefreshToken(true);
//		return defaultTokenServices;
//	}
	
	@Bean
	public RemoteTokenServices remoteTokenServices() {
		AccessTokenConverter tokenConverter = new DefaultAccessTokenConverter();
		String clientId = "5ce0e9ac811fd9c04543";
		String clientSecret = "b280853a74e6ae138ac23805092ddca670624ac9";
		
		RemoteTokenServices remoteTokenServices = new RemoteTokenServices() {

			@Override
			public OAuth2Authentication loadAuthentication(String accessToken) throws AuthenticationException, InvalidTokenException {
				MultiValueMap<String, String> formData = new LinkedMultiValueMap<String, String>();
				formData.add("token", accessToken);
				HttpHeaders headers = new HttpHeaders();
				String creds = String.format("%s:%s", clientId, clientSecret);
				try {
					headers.set("Authorization", "Basic " + new String(Base64.encode(creds.getBytes("UTF-8"))));
				}
				catch (UnsupportedEncodingException e) {
					throw new IllegalStateException("Could not convert String");
				}
				if (headers.getContentType() == null) {
					headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
				}
				@SuppressWarnings("rawtypes")
				Map map = oAuth2RestTemplate().exchange("https://api.github.com/applications/{clientId}/tokens/{accessToken}", HttpMethod.POST,
						new HttpEntity<MultiValueMap<String, String>>(formData, headers), Map.class, clientId, accessToken).getBody();
				
				if (map.containsKey("error")) {
					logger.debug("check_token returned error: " + map.get("error"));
					throw new InvalidTokenException(accessToken);
				}

				//Assert.state(map.containsKey("client_id"), "Client id must be present in response from auth server");
				return tokenConverter.extractAuthentication(map);
			}
			
		};
		remoteTokenServices.setRestTemplate(oAuth2RestTemplate());
		remoteTokenServices.setClientId("5ce0e9ac811fd9c04543");
		remoteTokenServices.setClientSecret("b280853a74e6ae138ac23805092ddca670624ac9");
		remoteTokenServices.setCheckTokenEndpointUrl("https://api.github.com/authorizations/{clientId}/tokens/{accessToken}");
		return remoteTokenServices;
	}
	
//	@Bean
	public OAuth2AuthenticationManager OAuth2AuthenticationManager() {
		OAuth2AuthenticationManager oAuth2AuthenticationManager = new OAuth2AuthenticationManager();
		oAuth2AuthenticationManager.setTokenServices(remoteTokenServices());
		return oAuth2AuthenticationManager;
	}
	
//	@Bean
	public OAuth2AuthenticationProcessingFilter oAuth2AuthenticationProcessingFilter() {
		OAuth2AuthenticationProcessingFilter oAuth2AuthenticationProcessingFilter = new OAuth2AuthenticationProcessingFilter();
		oAuth2AuthenticationProcessingFilter.setAuthenticationManager(OAuth2AuthenticationManager());
		return oAuth2AuthenticationProcessingFilter;
	}
}
