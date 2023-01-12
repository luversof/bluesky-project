package net.luversof.api.gate.user.client;

import java.util.Optional;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import net.luversof.api.gate.user.domain.User;

@FeignClient(value = "bluesky-api-user", path = "/api/user")
public interface UserClient {

	@GetMapping("/search/findByUserId")
	Optional<User> findByUserId(String userId);
}