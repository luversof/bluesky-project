package net.luversof.api.gate.user.client;

import java.util.Optional;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import net.luversof.api.gate.user.domain.User;

@FeignClient(value = "bluesky-api-user", path = "/api/user"/* , url="http://user.api.bluesky.local" */)
public interface UserClient {

	@GetMapping("/search/findByUserId")
	Optional<User> findByUserId(@RequestParam String userId);
}