package net.luversof.client.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "client.user")
public record ClientUserFeignProperties(FeignClient feignClient) {

	public static record FeignClient(String url) {}

}
