package net.luversof.security.oauth2.config;

import java.util.Arrays;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import org.springframework.security.oauth2.client.token.AccessTokenProvider;
import org.springframework.security.oauth2.client.token.AccessTokenProviderChain;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.client.token.grant.implicit.ImplicitAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordAccessTokenProvider;
import org.springframework.security.oauth2.common.AuthenticationScheme;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import lombok.SneakyThrows;
import net.luversof.security.oauth2.provider.token.GithubAccessTokenConverter;

@Configuration
public class OAuth2ClientGithubConfig {
	
	@Autowired
	private OAuth2ClientContext oAuth2ClientContext;

	/* (s) github OAuth */
	
	@Value("${oauth2.client.github.clientId}")
	private String githubClientId;
	
	@Value("${oauth2.client.github.clientSecret}")
	private String githubClientSecret;
	
	@Value("${oauth2.client.github.userAuthorizationUri}")
	private String githubUserAuthorizationUri;
	
	@Value("${oauth2.client.github.accessTokenUri}")
	private String githubAccessTokenUri;
	
	@Value("${oauth2.client.github.checkTokenEndpointUrl}")
	private String githubCheckTokenEndpointUrl;
	
	@Bean
	public OAuth2ProtectedResourceDetails githubResourceDetails() {
		AuthorizationCodeResourceDetails details = new AuthorizationCodeResourceDetails();
		details.setId("github");
		details.setClientId(githubClientId);
		details.setClientSecret(githubClientSecret);
		details.setUserAuthorizationUri(githubUserAuthorizationUri);
		details.setAccessTokenUri(githubAccessTokenUri);
		details.setScope(Arrays.asList("user"));
		details.setAuthenticationScheme(AuthenticationScheme.form);
		details.setClientAuthenticationScheme(AuthenticationScheme.form);
		return details;
	}
	
	@Bean
	public OAuth2RestTemplate githubRestTemplate() {
		OAuth2RestTemplate oAuth2RestTemplate = new OAuth2RestTemplate(githubResourceDetails(), oAuth2ClientContext);
		oAuth2RestTemplate.setAccessTokenProvider(getAccessTokenProvider());
		return oAuth2RestTemplate;
	}
	
	@Autowired
	private GithubAccessTokenConverter githubAccessTokenConverter;
	
	@Bean
	public ResourceServerTokenServices githubTokenServices(final OAuth2RestTemplate githubRestTemplate) {
		RemoteTokenServices remoteTokenServices = new RemoteTokenServices() {

			@Override
			public OAuth2Authentication loadAuthentication(String accessToken) throws AuthenticationException, InvalidTokenException {
				MultiValueMap<String, String> formData = new LinkedMultiValueMap<String, String>();
				formData.add("access_token", accessToken);
				HttpHeaders headers = new HttpHeaders();
				headers.set("Authorization", getAuthorizationHeader(githubClientId, githubClientSecret));
				if (headers.getContentType() == null) {
					headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
				}
				@SuppressWarnings("unchecked")
				Map<String, Object> map = githubRestTemplate.exchange(githubCheckTokenEndpointUrl, HttpMethod.GET,
						new HttpEntity<MultiValueMap<String, String>>(formData, headers), Map.class, githubClientId, accessToken).getBody();
				
				if (map.containsKey("error")) {
					logger.debug("check_token returned error: " + map.get("error"));
					throw new InvalidTokenException(accessToken);
				}

				//Assert.state(map.containsKey("client_id"), "Client id must be present in response from auth server");
				return githubAccessTokenConverter.extractAuthentication(map);
			}
			
		};
		remoteTokenServices.setRestTemplate(githubRestTemplate);
		remoteTokenServices.setClientId(githubClientId);
		remoteTokenServices.setClientSecret(githubClientSecret);
		remoteTokenServices.setCheckTokenEndpointUrl(githubCheckTokenEndpointUrl);
		remoteTokenServices.setAccessTokenConverter(githubAccessTokenConverter);
		return remoteTokenServices;
	}
	
	@Bean
	public OAuth2ClientAuthenticationProcessingFilter githubAuthenticationProcessingFilter(ResourceServerTokenServices githubTokenServices) {
		OAuth2ClientAuthenticationProcessingFilter githubAuthenticationProcessingFilter = new OAuth2ClientAuthenticationProcessingFilter("/oauth/githubAuthorizeResult");
		githubAuthenticationProcessingFilter.setRestTemplate(githubRestTemplate());
		githubAuthenticationProcessingFilter.setTokenServices(githubTokenServices);
		return githubAuthenticationProcessingFilter;
	}
	
	/* (e) github OAuth */
	
	@SneakyThrows
	private String getAuthorizationHeader(String clientId, String clientSecret) {
		String creds = String.format("%s:%s", clientId, clientSecret);
		return "Basic " + new String(Base64.encode(creds.getBytes("UTF-8")));
	}
	
	/**
	 * 2.0.8 이후  AuthorizationCodeAccessTokenProvider stateMandatory false 설정이 필요함. 
	 * @return
	 */
	private AccessTokenProvider getAccessTokenProvider() {
		AuthorizationCodeAccessTokenProvider authorizationCodeAccessTokenProvider = new AuthorizationCodeAccessTokenProvider();
		authorizationCodeAccessTokenProvider.setStateMandatory(false);
		return new AccessTokenProviderChain(Arrays.<AccessTokenProvider> asList(
				authorizationCodeAccessTokenProvider, new ImplicitAccessTokenProvider(),
				new ResourceOwnerPasswordAccessTokenProvider(), new ClientCredentialsAccessTokenProvider()));		
	}
}
