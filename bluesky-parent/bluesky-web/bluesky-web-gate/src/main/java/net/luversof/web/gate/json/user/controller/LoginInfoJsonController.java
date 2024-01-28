package net.luversof.web.gate.json.user.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.client.user.domain.LoginInfo;
import net.luversof.client.user.util.UserUtil;

@RestController
@RequestMapping(value= "/json/user/loginInfo", produces = MediaType.APPLICATION_JSON_VALUE)
public class LoginInfoJsonController {
	
	@GetMapping
	public LoginInfo loginInfo() {
		return UserUtil.getLoginInfo();
	}
	
}
