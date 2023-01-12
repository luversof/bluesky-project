package net.luversof.api.gate.user.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.gate.user.client.UserClient;
import net.luversof.api.gate.user.domain.User;


@RestController
@RequestMapping(value= "/api/user")
public class UserController {

	@Autowired
	private UserClient userClient;
	
	@GetMapping("/search/findByUserId")
	public Optional<User> findByUserId(String userId) {
		return userClient.findByUserId(userId);
	}
}
