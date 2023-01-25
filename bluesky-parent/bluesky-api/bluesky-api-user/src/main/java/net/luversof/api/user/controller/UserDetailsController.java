package net.luversof.api.user.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/user/userDetails", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserDetailsController {

	@Autowired
	private UserDetailsManager userDetailsManager;
	
	@GetMapping("/search/loadUserByUsername")
	public UserDetails loadUserByUsername(String username) {
		return userDetailsManager.loadUserByUsername(username);
	}
	
	@PostMapping
	public UserDetails createUser(@RequestBody User user) {
		userDetailsManager.createUser(user);
		return user;
	}
	
	@PutMapping
	public UserDetails updateUser(@RequestBody User user) {
		userDetailsManager.updateUser(user);
		return user;
	}
	
	@DeleteMapping
	public void deleteUser(String username) {
		userDetailsManager.deleteUser(username);
	}
}
