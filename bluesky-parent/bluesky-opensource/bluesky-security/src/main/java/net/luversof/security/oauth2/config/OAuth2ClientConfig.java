package net.luversof.security.oauth2.config;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Map;

import net.luversof.security.oauth2.provider.token.GithubAccessTokenConverter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.common.AuthenticationScheme;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.AccessTokenConverter;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Configuration
@EnableOAuth2Client
@PropertySource(name = "oauth2ClientProp", value = "classpath:config/oauth2-client.properties")
public class OAuth2ClientConfig {
	
	@Autowired
	private OAuth2ClientContext oAuth2ClientContext;
	
	@Bean
	public OAuth2ProtectedResourceDetails githubResourceDetails() {
		AuthorizationCodeResourceDetails details = new AuthorizationCodeResourceDetails();
		details.setId("github");
		details.setClientId("5ce0e9ac811fd9c04543");
		details.setClientSecret("b280853a74e6ae138ac23805092ddca670624ac9");
		details.setUserAuthorizationUri("https://github.com/login/oauth/authorize");
		details.setAccessTokenUri("https://github.com/login/oauth/access_token");
		details.setScope(Arrays.asList("user"));
//		details.setAuthenticationScheme(AuthenticationScheme.form);
		details.setClientAuthenticationScheme(AuthenticationScheme.form);
		return details;
	}
	
	@Bean
	public OAuth2ProtectedResourceDetails facebookResourceDetails() {
		AuthorizationCodeResourceDetails details = new AuthorizationCodeResourceDetails();
		details.setId("facebook");
		details.setClientId("124755777691522");
		details.setClientSecret("3e543ae36e0185e44b1504ec1ec717c9");
		details.setUserAuthorizationUri("https://www.facebook.com/dialog/oauth");
		details.setAccessTokenUri("https://graph.facebook.com/oauth/access_token");
		details.setPreEstablishedRedirectUri("http://localhost:8081/oauth/authorizeResult");
		details.setScope(Arrays.asList("email"));
//		details.setAuthenticationScheme(AuthenticationScheme.form);
		details.setClientAuthenticationScheme(AuthenticationScheme.form);
		return details;
	}
	
	@Bean
	public OAuth2RestTemplate githubRestTemplate() {
		return new OAuth2RestTemplate(githubResourceDetails(), oAuth2ClientContext);
	}
	
	@Bean
	public OAuth2RestTemplate facebookRestTemplate() {
		return new OAuth2RestTemplate(facebookResourceDetails(), oAuth2ClientContext);
	}
	
	@Bean
	public OAuth2ClientAuthenticationProcessingFilter githubAuthenticationProcessingFilter() {
		OAuth2ClientAuthenticationProcessingFilter githubAuthenticationProcessingFilter = new OAuth2ClientAuthenticationProcessingFilter("/oauth/githubAuthorizeResult");
		githubAuthenticationProcessingFilter.setRestTemplate(githubRestTemplate());
		githubAuthenticationProcessingFilter.setTokenServices(githubTokenServices());
		return githubAuthenticationProcessingFilter;
	}
	
	@Bean
	public ResourceServerTokenServices githubTokenServices() {
		AccessTokenConverter accessTokenConverter = new GithubAccessTokenConverter();
		String clientId = "5ce0e9ac811fd9c04543";
		String clientSecret = "b280853a74e6ae138ac23805092ddca670624ac9";
		String checkTokenEndpointUrl = "https://api.github.com/applications/{clientId}/tokens/{accessToken}";
		
		RemoteTokenServices githubTokenServices = new RemoteTokenServices() {

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
				@SuppressWarnings("unchecked")
				Map<String, Object> map = githubRestTemplate().exchange(checkTokenEndpointUrl, HttpMethod.GET,
						new HttpEntity<MultiValueMap<String, String>>(formData, headers), Map.class, clientId, accessToken).getBody();
				
				if (map.containsKey("error")) {
					logger.debug("check_token returned error: " + map.get("error"));
					throw new InvalidTokenException(accessToken);
				}

				//Assert.state(map.containsKey("client_id"), "Client id must be present in response from auth server");
				return accessTokenConverter.extractAuthentication(map);
			}
			
		};
		githubTokenServices.setRestTemplate(githubRestTemplate());
		githubTokenServices.setClientId(clientId);
		githubTokenServices.setClientSecret(clientSecret);
		githubTokenServices.setCheckTokenEndpointUrl(checkTokenEndpointUrl);
		githubTokenServices.setAccessTokenConverter(accessTokenConverter);
		return githubTokenServices;
	}
}
