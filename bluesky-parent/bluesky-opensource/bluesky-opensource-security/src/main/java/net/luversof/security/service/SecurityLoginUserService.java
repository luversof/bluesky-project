//package net.luversof.security.service;
//
//import java.util.Optional;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
//import org.springframework.stereotype.Service;
//
//import io.github.luversof.boot.exception.BlueskyException;
//import net.luversof.core.exception.CoreErrorCode;
//import net.luversof.security.core.userdetails.BlueskyUser;
//import net.luversof.user.domain.User;
//import net.luversof.user.domain.UserType;
//import net.luversof.user.service.UserService;
//
//@Service
//public class SecurityLoginUserService {
//	
//	@Autowired
//	private UserService userService;
//
//	public String getUserId() {
//		return getUser().orElseThrow(() -> new BlueskyException(CoreErrorCode.NOT_EXIST_USER_ID)).getUserId();
//	}
//
//	public Optional<User> getUser() {
//		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//		if (authentication == null) {
//			return Optional.empty();
//		}
//		if (authentication.getPrincipal() instanceof BlueskyUser blueskyUser) {
//			return Optional.of(blueskyUser.getUser());
//		}
//		
//		if (authentication instanceof OAuth2AuthenticationToken oAuth2AuthenticationToken) {
//			return userService.findByExternalIdAndUserType(oAuth2AuthenticationToken.getPrincipal().getName(), UserType.findByName(oAuth2AuthenticationToken.getAuthorizedClientRegistrationId()));
//		}
//		return Optional.empty();
//	}
//}
