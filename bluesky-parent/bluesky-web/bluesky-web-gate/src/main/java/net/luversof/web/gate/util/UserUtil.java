package net.luversof.web.gate.util;

import java.util.UUID;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import lombok.experimental.UtilityClass;

/**
 * 사용자 정보를 가져오는 유틸리티 클래스
 * Token Exchange 패턴에서 사용
 */
@UtilityClass
public class UserUtil {

    /**
     * 현재 인증된 사용자의 UUID를 반환
     * 
     * @return 사용자 UUID, 인증되지 않은 경우 null
     */
    public static UUID getUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        // JWT Token에서 추출 (Token Exchange 후)
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String sub = jwt.getSubject();
            try {
                return UUID.fromString(sub);
            } catch (IllegalArgumentException e) {
                // sub가 UUID가 아닌 경우
                return null;
            }
        }

        // OAuth2 로그인 (GitHub 로그인 직후)
        if (authentication instanceof OAuth2AuthenticationToken oauth2Auth) {
            OAuth2User principal = oauth2Auth.getPrincipal();
            // GitHub OAuth의 경우 sub 대신 id를 사용할 수 있음
            Object subAttr = principal.getAttribute("sub");
            if (subAttr != null) {
                try {
                    return UUID.fromString(subAttr.toString());
                } catch (IllegalArgumentException e) {
                    // sub가 UUID가 아닌 경우
                }
            }
        }

        return null;
    }

    /**
     * 현재 인증된 사용자의 username을 반환
     * 
     * @return username, 인증되지 않은 경우 null
     */
    public static String getUsername() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        return authentication.getName();
    }

    /**
     * 사용자가 로그인했는지 확인
     * 
     * @return 로그인 여부
     */
    public static boolean isAuthenticated() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
