package net.luversof.security.oauth2.config;

import java.util.Arrays;
import java.util.Base64;
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
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import lombok.SneakyThrows;
import net.luversof.security.oauth2.provider.token.FacebookAccessTokenConverter;

@Configuration
public class OAuth2CliehtFacebookConfig {

	@Autowired
	private OAuth2ClientContext oAuth2ClientContext;
	
	/* (s) facebook OAuth */
	
	@Value("${oauth2.client.facebook.clientId}")
	private String facebookClientId;
	
	@Value("${oauth2.client.facebook.clientSecret}")
	private String facebookClientSecret;
	
	@Value("${oauth2.client.facebook.userAuthorizationUri}")
	private String facebookUserAuthorizationUri;
	
	@Value("${oauth2.client.facebook.accessTokenUri}")
	private String facebookAccessTokenUri;
	
	@Value("${oauth2.client.facebook.checkTokenEndpointUrl}")
	private String facebookCheckTokenEndpointUrl;

	
	@Bean
//	@ConfigurationProperties(prefix = "oauth2.client.facebook")
	public OAuth2ProtectedResourceDetails facebookResourceDetails() {
		AuthorizationCodeResourceDetails details = new AuthorizationCodeResourceDetails();
		details.setId("facebook");
		details.setClientId(facebookClientId);
		details.setClientSecret(facebookClientSecret);
		details.setUserAuthorizationUri(facebookUserAuthorizationUri);
		details.setAccessTokenUri(facebookAccessTokenUri);
		details.setScope(Arrays.asList("email"));
		details.setAuthenticationScheme(AuthenticationScheme.form);
		details.setClientAuthenticationScheme(AuthenticationScheme.form);
		return details;
	}

	@Bean
	public OAuth2RestTemplate facebookRestTemplate() {
		OAuth2RestTemplate oAuth2RestTemplate = new OAuth2RestTemplate(facebookResourceDetails(), oAuth2ClientContext);
		oAuth2RestTemplate.setAccessTokenProvider(getAccessTokenProvider());
		return oAuth2RestTemplate;
	}
	
	@Autowired
	private FacebookAccessTokenConverter facebookAccessTokenConverter;
	
	@Bean
	public ResourceServerTokenServices facebookTokenServices(final OAuth2RestTemplate facebookRestTemplate) {
		RemoteTokenServices remoteTokenServices = new RemoteTokenServices() {

			@Override
			public OAuth2Authentication loadAuthentication(String accessToken) throws AuthenticationException, InvalidTokenException {
				MultiValueMap<String, String> formData = new LinkedMultiValueMap<String, String>();
				formData.add("access_token", accessToken);
				HttpHeaders headers = new HttpHeaders();
				headers.set("Authorization", getAuthorizationHeader(facebookClientId, facebookClientSecret));
				if (headers.getContentType() == null) {
					headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
				}
				@SuppressWarnings("unchecked")
				Map<String, Object> map = facebookRestTemplate.exchange(facebookCheckTokenEndpointUrl, HttpMethod.GET,
						new HttpEntity<MultiValueMap<String, String>>(formData, headers), Map.class, accessToken).getBody();

				if (map.containsKey("error")) {
					logger.debug("check_token returned error: " + map.get("error"));
					throw new InvalidTokenException(accessToken);
				}
				@SuppressWarnings("unchecked")
				Map<String, Object> me = facebookRestTemplate.getForObject("https://graph.facebook.com/me", Map.class);
				map.putAll(me);
//				Assert.state(map.containsKey("client_id"), "Client id must be present in response from auth server");
				return facebookAccessTokenConverter.extractAuthentication(map);
			}
			
		};
		remoteTokenServices.setRestTemplate(facebookRestTemplate);
		remoteTokenServices.setClientId(facebookClientId);
		remoteTokenServices.setClientSecret(facebookClientSecret);
		remoteTokenServices.setCheckTokenEndpointUrl(facebookCheckTokenEndpointUrl);
		return remoteTokenServices;
	}
	
	
	@Bean
	public OAuth2ClientAuthenticationProcessingFilter facebookAuthenticationProcessingFilter(ResourceServerTokenServices facebookTokenServices) {
		OAuth2ClientAuthenticationProcessingFilter facebookAuthenticationProcessingFilter = new OAuth2ClientAuthenticationProcessingFilter("/oauth/facebookAuthorizeResult");
		facebookAuthenticationProcessingFilter.setRestTemplate(facebookRestTemplate());
		facebookAuthenticationProcessingFilter.setTokenServices(facebookTokenServices);
		{
			SavedRequestAwareAuthenticationSuccessHandler savedRequestAwareAuthenticationSuccessHandler = new SavedRequestAwareAuthenticationSuccessHandler();
			savedRequestAwareAuthenticationSuccessHandler.setUseReferer(true);
			facebookAuthenticationProcessingFilter.setAuthenticationSuccessHandler(savedRequestAwareAuthenticationSuccessHandler);
		}
		return facebookAuthenticationProcessingFilter;
	}
	
	/* (e) facebook OAuth */
	
	@SneakyThrows
	private String getAuthorizationHeader(String clientId, String clientSecret) {
		String creds = String.format("%s:%s", clientId, clientSecret);
		return "Basic " + new String(Base64.getEncoder().encodeToString(creds.getBytes("UTF-8")));
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
