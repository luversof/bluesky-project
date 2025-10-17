package net.luversof.api.user.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.Setter;
import net.luversof.api.user.domain.UserInfo;
import net.luversof.api.user.service.UserInfoService;

@RestController
@RequestMapping(value = "/api/userInfo", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserInfoController {

	@Setter(onMethod_ = @Autowired)
	private UserInfoService userInfoService;
	
	@GetMapping("/search/findByUsername/{userName}")
	public Optional<UserInfo> findByUsername(@PathVariable String userName) {
		return userInfoService.findByUsername(userName);
	}

}
