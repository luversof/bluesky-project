package net.luversof.web.gate.user.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.web.gate.user.domain.LoginInfo;
import net.luversof.web.gate.user.util.UserUtil;

@RestController
@RequestMapping(value= "/api/user/loginInfo", produces = MediaType.APPLICATION_JSON_VALUE)
public class LoginInfoController {
	
	@GetMapping
	public LoginInfo loginInfo() {
		return UserUtil.getLoginInfo();
	}
	
}
