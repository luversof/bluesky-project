package net.luversof.web.gate.user.controller;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import net.luversof.client.user.util.UserUtil;

@Controller
public class UserController {

	@GetMapping("/user/info")
	@ResponseBody
	public Map<String, Object> userInfo(@AuthenticationPrincipal OAuth2User principal) {
		if (principal == null) {
			return Map.of("error", "Not authenticated");
		}

		return Map.of(
				"attributes", principal.getAttributes(), 
				"authorities", principal.getAuthorities(), 
				"userId", UserUtil.getUserId() != null ? UserUtil.getUserId().toString() : "null", 
				"username", UserUtil.getUsername() != null ? UserUtil.getUsername() : "null");
	}

	@GetMapping("/login")
	public String login() {
		return "redirect:https://dev.bluesky.local:40131/login";
	}

}
