package net.luversof.api.gate.user.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import net.luversof.api.gate.user.client.UserClient;
import net.luversof.api.gate.user.domain.User;


@Slf4j
@RestController
@RequestMapping(value= "/api/user")
public class UserController {

	@Autowired
	private UserClient userClient;
	
	@GetMapping("/search/findByUserId")
	public Optional<User> findByUserId(String userId) {
		var user = userClient.findByUserId(userId);
		log.debug("user : {}", user);
		return user;
	}
}
