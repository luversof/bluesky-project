package net.luversof.api.user.controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.Setter;
import net.luversof.api.user.domain.UserInfo;
import net.luversof.api.user.service.UserInfoService;

@RestController
@RequestMapping(value = "/api/userInfo", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserInfoController {

	@Setter(onMethod_ = @Autowired)
	private UserInfoService userInfoService;

	@GetMapping("/{id}")
	public Optional<UserInfo> findById(@PathVariable UUID id) {
		return userInfoService.findById(id);
	}

	@GetMapping("/search/findByIdIn")
	public List<UserInfo> findByIdIn(@RequestParam("ids") List<UUID> ids) {
		return userInfoService.findByIdIn(ids);
	}

	@GetMapping("/search/findByUsername/{userName}")
	public Optional<UserInfo> findByUsername(@PathVariable String userName) {
		return userInfoService.findByUsername(userName);
	}

	@GetMapping("/search/findByProvider")
	public Optional<UserInfo> findByProviderAndProviderId(
			@RequestParam("provider") String provider,
			@RequestParam("providerId") String providerId) {
		return userInfoService.findByProviderAndProviderId(provider, providerId);
	}

	@PostMapping("/oauth2")
	public UserInfo saveOAuth2User(@RequestBody OAuth2UserRequest request) {
		return userInfoService.saveOAuth2User(
			request.provider(),
			request.providerId(),
			request.username(),
			request.email(),
			request.avatarUrl()
		);
	}

	record OAuth2UserRequest(
		String provider,
		String providerId,
		String username,
		String email,
		String avatarUrl
	) {}

}
