package net.luversof.web.gate.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * GitHub OAuth 로그인 성공 후 Token Exchange 처리
 */
@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Value("${spring.security.oauth2.client.provider.bluesky.token-uri:https://dev.bluesky.local:40131/oauth2/token}")
    private String tokenUri;

    @Value("${spring.security.oauth2.client.registration.bluesky.client-id:bluesky-web-gate}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.bluesky.client-secret:secret}")
    private String clientSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    public OAuth2LoginSuccessHandler() {
        setDefaultTargetUrl("/");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            String provider = oauthToken.getAuthorizedClientRegistrationId();

            // GitHub에서 받은 access token 추출
            String githubAccessToken = extractAccessToken(request, provider);

            if (githubAccessToken != null) {
                // Token Exchange 요청
                String jwtToken = exchangeToken(githubAccessToken);

                if (jwtToken != null) {
                    // JWT를 세션이나 쿠키에 저장
                    request.getSession().setAttribute("JWT_TOKEN", jwtToken);
                    // 또는 response에 쿠키로 설정
                    // Cookie cookie = new Cookie("JWT_TOKEN", jwtToken);
                    // cookie.setHttpOnly(true);
                    // cookie.setSecure(true);
                    // cookie.setPath("/");
                    // response.addCookie(cookie);
                }
            }
        }

        super.onAuthenticationSuccess(request, response, authentication);
    }

    private String extractAccessToken(HttpServletRequest request, String provider) {
        // OAuth2AuthorizedClient에서 access token 추출
        // 실제 구현에서는 OAuth2AuthorizedClientService를 사용해야 함
        // 여기서는 간단히 null 반환
        return "github_access_token_placeholder";
    }

    private String exchangeToken(String subjectToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth(clientId, clientSecret);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange");
            body.add("subject_token", subjectToken);
            body.add("subject_token_type", "urn:ietf:params:oauth:token-type:access_token");

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

            JsonNode response = restTemplate.postForObject(tokenUri, requestEntity, JsonNode.class);

            if (response != null && response.has("access_token")) {
                return response.get("access_token").asText();
            }
        } catch (Exception e) {
            // 로깅
            e.printStackTrace();
        }
        return null;
    }
}
