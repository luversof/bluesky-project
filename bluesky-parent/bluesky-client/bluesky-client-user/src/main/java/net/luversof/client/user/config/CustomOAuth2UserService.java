package net.luversof.client.user.config;

import java.util.HashMap;
import java.util.Map;
import net.luversof.client.user.httpexchange.UserInfoApiClient;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserInfoApiClient userInfoApiClient;
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    public CustomOAuth2UserService(UserInfoApiClient userInfoApiClient) {
        this.userInfoApiClient = userInfoApiClient;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String provider = normalizeProvider(registrationId);

        // GitHub/Kakao OAuth2User attributes
        Object idAttr = oauth2User.getAttribute("id");
        String providerId = idAttr != null ? idAttr.toString() : null;
        String username = oauth2User.getAttribute("login"); // GitHub
        if (username == null) {
            username = oauth2User.getAttribute("preferred_username"); // Keycloak etc
        }
        if (username == null) {
            username = oauth2User.getName();
        }

        String email = oauth2User.getAttribute("email");
        String avatarUrl = oauth2User.getAttribute("avatar_url");

        UserInfoApiClient.UserInfoResponse userInfo = null;
        try {
            // Save or Update user info
            var request =
                    new UserInfoApiClient.SaveOAuth2UserRequest(
                            provider, providerId, username, email, avatarUrl);

            userInfo = userInfoApiClient.saveOAuth2User(request);
        } catch (Exception e) {
            // Log error but allow login to proceed
            e.printStackTrace();
        }

        Map<String, Object> attributes = new HashMap<>(oauth2User.getAttributes());
        if (userInfo != null) {
            attributes.put("userInfo", userInfo);
        }

        String userNameAttributeName =
                userRequest
                        .getClientRegistration()
                        .getProviderDetails()
                        .getUserInfoEndpoint()
                        .getUserNameAttributeName();

        return new DefaultOAuth2User(
                oauth2User.getAuthorities(), attributes, userNameAttributeName);
    }

    private String normalizeProvider(String provider) {
        if (provider == null) {
            return null;
        }
        if (provider.startsWith("github")) {
            return "github";
        }
        if (provider.startsWith("kakao")) {
            return "kakao";
        }
        return provider;
    }
}
