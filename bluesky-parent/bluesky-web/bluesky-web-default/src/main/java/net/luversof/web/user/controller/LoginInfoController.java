package net.luversof.web.user.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.luversof.boot.autoconfigure.security.annotation.BlueskyPreAuthorize;
import lombok.Data;
import net.luversof.security.service.SecurityLoginUserService;
import net.luversof.user.domain.User;
import net.luversof.user.service.UserService;

@RestController
@RequestMapping(value= "/api/user/loginInfo", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
public class LoginInfoController {
	
	@Autowired
	private SecurityLoginUserService securityLoginUserService;

	@BlueskyPreAuthorize
	@GetMapping
	public LoginInfo loginInfo() {
		
		User user = securityLoginUserService.getUser().orElse(null);
		
		LoginInfo loginInfo = new LoginInfo();
		
		if (user == null) {
			return loginInfo;
		}
		
		loginInfo.setId(user.getUserId());
		loginInfo.setName(user.getUserName());
		
		return loginInfo;
	}
	
	@Autowired
	private UserService userService;
	
	@GetMapping("/deleteTest")
	public void test(String userId) {
		userService.deleteByUserId(userId);
	}
	
	@Data
	public class LoginInfo {
		private String id;
		private String name;
	}
}
