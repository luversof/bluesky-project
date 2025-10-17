package net.luversof.client.user.openfeign;

import java.util.Optional;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import net.luversof.client.user.config.UserClientFeignConfig;
import net.luversof.client.user.domain.UserInfo;

@FeignClient(value = "bluesky-api-user", contextId = "api-user-userInfo", path = "/api/userInfo", url = "${gate.feign-client.url.user:}", configuration = UserClientFeignConfig.class)
public interface UserInfoClient {

	@GetMapping("/search/findByUsername/{userName}")
	Optional<UserInfo> findByUsername(@PathVariable String userName);

}
