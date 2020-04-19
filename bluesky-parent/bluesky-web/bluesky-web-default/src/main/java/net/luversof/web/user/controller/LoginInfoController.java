package net.luversof.web.user.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.Data;
import net.luversof.security.service.SecurityLoginUserService;
import net.luversof.user.domain.User;

@RestController
@RequestMapping(value= "/api/user/loginInfo")
public class LoginInfoController {
	
	@Autowired
	private SecurityLoginUserService securityLoginUserService;

	@GetMapping
	public LoginInfo loginInfo() {
		
		User user = securityLoginUserService.getUser().orElse(null);
		
		LoginInfo loginInfo = new LoginInfo();
		
		if (user == null) {
			return loginInfo;
		}
		
		loginInfo.setLogin(true);
		loginInfo.setId(user.getId());
		loginInfo.setName(user.getUsername());
		
		return loginInfo;
	}
	
	@Data
	public class LoginInfo {
		private boolean isLogin;
		private UUID id;
		private String name;
	}
}
