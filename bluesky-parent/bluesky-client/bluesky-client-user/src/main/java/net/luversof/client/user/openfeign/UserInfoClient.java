package net.luversof.client.user.openfeign;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import net.luversof.client.user.config.UserClientFeignConfig;
import net.luversof.client.user.domain.UserInfo;

@FeignClient(value = "bluesky-api-user", contextId = "api-user-userInfo", path = "/api/userInfo", url = "${gate.feign-client.url.user:}", configuration = UserClientFeignConfig.class)
public interface UserInfoClient {

	@GetMapping("/{id}")
	Optional<UserInfo> findById(@PathVariable UUID id);

	@GetMapping("/search/findByIdIn")
	List<UserInfo> findByIdIn(@RequestParam("ids") List<UUID> ids);

	@GetMapping("/search/findByUsername/{userName}")
	Optional<UserInfo> findByUsername(@PathVariable String userName);

}
