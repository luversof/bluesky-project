package net.luversof.security.oauth2.config;

import java.util.Arrays;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.security.oauth2.client.token.AccessTokenProvider;
import org.springframework.security.oauth2.client.token.AccessTokenProviderChain;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.client.token.grant.implicit.ImplicitAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordAccessTokenProvider;
import org.springframework.security.oauth2.common.AuthenticationScheme;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import lombok.SneakyThrows;
import net.luversof.security.oauth2.provider.token.BattleNetAccessTokenConverter;
import net.luversof.security.oauth2.provider.token.FacebookAccessTokenConverter;
import net.luversof.security.oauth2.provider.token.GithubAccessTokenConverter;

@Configuration
@EnableOAuth2Client
@PropertySource(name = "oauth2ClientProp", value = "classpath:config/oauth2-client.properties")
public class OAuth2ClientConfig {

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
		return facebookAuthenticationProcessingFilter;
	}
	
	/* (e) facebook OAuth */
	
	/* (s) battleNet OAuth */
	
	@Value("${oauth2.client.battleNet.clientId}")
	private String battleNetClientId;
	
	@Value("${oauth2.client.battleNet.clientSecret}")
	private String battleNetClientSecret;
	
	@Value("${oauth2.client.battleNet.userAuthorizationUri}")
	private String battleNetUserAuthorizationUri;
	
	@Value("${oauth2.client.battleNet.accessTokenUri}")
	private String battleNetAccessTokenUri;
	
	@Value("${oauth2.client.battleNet.checkTokenEndpointUrl}")
	private String battleNetCheckTokenEndpointUrl;
	
	
	@Bean
	public OAuth2ProtectedResourceDetails battleNetResourceDetails() {
		AuthorizationCodeResourceDetails details = new AuthorizationCodeResourceDetails();
		details.setId("battleNet");
		details.setClientId(battleNetClientId);
		details.setClientSecret(battleNetClientSecret);
		details.setUserAuthorizationUri(battleNetUserAuthorizationUri);
		details.setAccessTokenUri(battleNetAccessTokenUri);
		details.setScope(Arrays.asList("email"));
		details.setAuthenticationScheme(AuthenticationScheme.form);
		details.setClientAuthenticationScheme(AuthenticationScheme.form);
		return details;
	}

	@Bean
	public OAuth2RestTemplate battleNetRestTemplate() {
		OAuth2RestTemplate oAuth2RestTemplate = new OAuth2RestTemplate(battleNetResourceDetails(), oAuth2ClientContext);
		oAuth2RestTemplate.setAccessTokenProvider(getAccessTokenProvider());
		return oAuth2RestTemplate;
	}
	
	
	@Autowired
	BattleNetAccessTokenConverter battleNetAccessTokenConverter;
	
	@Bean
	public ResourceServerTokenServices battleNetTokenServices(OAuth2RestTemplate battleNetRestTemplate) {
		RemoteTokenServices remoteTokenServices = new RemoteTokenServices() {

			@Override
			public OAuth2Authentication loadAuthentication(String accessToken) throws AuthenticationException, InvalidTokenException {
				MultiValueMap<String, String> formData = new LinkedMultiValueMap<String, String>();
				formData.add("token", accessToken);
				HttpHeaders headers = new HttpHeaders();
				headers.set("Authorization", getAuthorizationHeader(battleNetClientId, battleNetClientSecret));
				if (headers.getContentType() == null) {
					headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
				}
				@SuppressWarnings("unchecked")
				Map<String, Object> map = battleNetRestTemplate.exchange(battleNetCheckTokenEndpointUrl, HttpMethod.GET,
						new HttpEntity<MultiValueMap<String, String>>(formData, headers), Map.class, accessToken).getBody();

				if (map.containsKey("code")) {
					logger.debug("check_token returned error: " + map.get("detail"));
					throw new InvalidTokenException(accessToken);
				}
				
				@SuppressWarnings("unchecked")
				Map<String, Object> me = battleNetRestTemplate.getForObject("https://kr.api.battle.net/account/user?access_token={accessToken}", Map.class, accessToken);
				map.putAll(me);
				return battleNetAccessTokenConverter.extractAuthentication(map);
			}
			
		};
		remoteTokenServices.setRestTemplate(battleNetRestTemplate());
		remoteTokenServices.setClientId(battleNetClientId);
		remoteTokenServices.setClientSecret(battleNetClientSecret);
		remoteTokenServices.setCheckTokenEndpointUrl(battleNetCheckTokenEndpointUrl);
		return remoteTokenServices;
	}
	
	
	@Bean
	public OAuth2ClientAuthenticationProcessingFilter battleNetAuthenticationProcessingFilter(ResourceServerTokenServices battleNetTokenServices) {
		OAuth2ClientAuthenticationProcessingFilter battleNetAuthenticationProcessingFilter = new OAuth2ClientAuthenticationProcessingFilter("/oauth/battleNetAuthorizeResult");
		battleNetAuthenticationProcessingFilter.setRestTemplate(battleNetRestTemplate());
		battleNetAuthenticationProcessingFilter.setTokenServices(battleNetTokenServices);
		SimpleUrlAuthenticationSuccessHandler simpleUrlAuthenticationSuccessHandler = new SimpleUrlAuthenticationSuccessHandler("/battleNet/d3/index");
		battleNetAuthenticationProcessingFilter.setAuthenticationSuccessHandler(simpleUrlAuthenticationSuccessHandler);
		return battleNetAuthenticationProcessingFilter;
	}
	
	/* (e) battleNet OAuth */
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
