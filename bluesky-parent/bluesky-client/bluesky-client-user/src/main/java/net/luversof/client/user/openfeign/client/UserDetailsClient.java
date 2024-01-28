package net.luversof.client.user.openfeign.client;

import java.util.Optional;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import net.luversof.client.user.openfeign.config.UserClientFeignConfig;


@FeignClient(value = "bluesky-api-user", contextId = "api-user-userDetails", path = "/api/user/userDetails", url = "${gate.feign-client.url.user:}", configuration = UserClientFeignConfig.class)
public interface UserDetailsClient {

	@GetMapping("/search/loadUserByUsername")
	public Optional<UserDetails> loadUserByUsername(@RequestParam String username);
	
	@PostMapping
	public Optional<UserDetails> createUser(@RequestBody User user);
	
	@PutMapping
	public Optional<UserDetails> updateUser(@RequestBody User user);
	
	@DeleteMapping
	public void deleteUser(@RequestParam String username);

}