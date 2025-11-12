package net.luversof.api.user.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Token Exchange Grant Type을 위한 Converter
 * RFC 8693: OAuth 2.0 Token Exchange
 */
public class OAuth2TokenExchangeGrantAuthenticationConverter implements AuthenticationConverter {

    private static final AuthorizationGrantType TOKEN_EXCHANGE_GRANT_TYPE = new AuthorizationGrantType(
            "urn:ietf:params:oauth:grant-type:token-exchange");

    @Nullable
    @Override
    public Authentication convert(HttpServletRequest request) {
        // grant_type 확인
        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
        if (!TOKEN_EXCHANGE_GRANT_TYPE.getValue().equals(grantType)) {
            return null;
        }

        MultiValueMap<String, String> parameters = OAuth2EndpointUtils.getParameters(request);

        // subject_token (필수)
        String subjectToken = parameters.getFirst("subject_token");
        if (!StringUtils.hasText(subjectToken) ||
                parameters.get("subject_token").size() != 1) {
            throwError(OAuth2ErrorCodes.INVALID_REQUEST, "subject_token");
        }

        // subject_token_type (필수)
        String subjectTokenType = parameters.getFirst("subject_token_type");
        if (!StringUtils.hasText(subjectTokenType) ||
                parameters.get("subject_token_type").size() != 1) {
            throwError(OAuth2ErrorCodes.INVALID_REQUEST, "subject_token_type");
        }

        // 추가 파라미터
        Map<String, Object> additionalParameters = new HashMap<>();
        parameters.forEach((key, value) -> {
            if (!key.equals(OAuth2ParameterNames.GRANT_TYPE) &&
                    !key.equals(OAuth2ParameterNames.CLIENT_ID) &&
                    !key.equals("subject_token") &&
                    !key.equals("subject_token_type")) {
                additionalParameters.put(key, value.get(0));
            }
        });

        additionalParameters.put("subject_token", subjectToken);
        additionalParameters.put("subject_token_type", subjectTokenType);

        return new OAuth2TokenExchangeAuthenticationToken(
                OAuth2EndpointUtils.getAuthenticatedClientElseThrowInvalidClient(request),
                subjectToken,
                subjectTokenType,
                additionalParameters);
    }

    private static void throwError(String errorCode, String parameterName) {
        OAuth2Error error = new OAuth2Error(errorCode, "OAuth 2.0 Parameter: " + parameterName,
                "https://datatracker.ietf.org/doc/html/rfc8693#section-2.1");
        throw new OAuth2AuthenticationException(error);
    }
}
