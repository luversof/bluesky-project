package net.luversof.api.user.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.user.domain.User;
import net.luversof.user.domain.UserType;
import net.luversof.user.service.UserService;

@RestController
@RequestMapping(value = "/api/user", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserController {

	@Autowired
	private UserService userService;
	
	
	@PostMapping
	public User addUser(@RequestBody @Validated(User.Create.class) User user) {
		return userService.addUser(user);
	}
	
	@GetMapping
	public Optional<User> findByUserId(String userId) {
		return userService.findByUserId(userId);
	}
	
	@GetMapping
	public Optional<User> findByUsername(String userName) {
		return userService.findByUsername(userName);
	}
	
	@GetMapping
	public Optional<User> findByUsername(String externalId, UserType userType) {
		return userService.findByExternalIdAndUserType(externalId, userType);
	}
	
	@DeleteMapping
	public void deleteByUserId(String userId) {
		userService.deleteByUserId(userId);
	}

}
