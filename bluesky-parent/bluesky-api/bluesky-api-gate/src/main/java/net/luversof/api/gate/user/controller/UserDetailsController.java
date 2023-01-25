package net.luversof.api.gate.user.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.gate.user.client.UserDetailsClient;

@RestController
@RequestMapping(value = "/api/user/userDetails", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserDetailsController {

	@Autowired
	private UserDetailsClient userDetailsClient;
	
	@GetMapping("/search/loadUserByUsername")
	public Optional<UserDetails> loadUserByUsername(String username) {
		return userDetailsClient.loadUserByUsername(username);
	}
	
	@PostMapping
	public Optional<UserDetails> createUser(@RequestBody User user) {
		return userDetailsClient.createUser(user);
	}
	
	@PutMapping
	public Optional<UserDetails> updateUser(@RequestBody User user) {
		return userDetailsClient.updateUser(user);
	}
	
	@DeleteMapping
	public void deleteUser(String username) {
		userDetailsClient.deleteUser(username);
	}
}
