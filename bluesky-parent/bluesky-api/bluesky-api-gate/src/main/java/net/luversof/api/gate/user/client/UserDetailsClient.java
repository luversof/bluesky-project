package net.luversof.api.gate.user.client;

import java.util.Optional;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import net.luversof.api.gate.user.domain.BlueskyUserDetails;

@FeignClient(value = "bluesky-api-user", path = "/api/user/userDetails", url = "${gate.feign-client.url.user:}")
public interface UserDetailsClient {

	@GetMapping("/search/loadUserByUsername")
	public Optional<BlueskyUserDetails> loadUserByUsername(@RequestParam String username);
	
	@PostMapping
	public Optional<BlueskyUserDetails> createUser(@RequestBody UserDetails userDetails);
	
	@PutMapping
	public Optional<BlueskyUserDetails> updateUser(@RequestBody UserDetails userDetails);
	
	@DeleteMapping
	public void deleteUser(@RequestParam String username);

}