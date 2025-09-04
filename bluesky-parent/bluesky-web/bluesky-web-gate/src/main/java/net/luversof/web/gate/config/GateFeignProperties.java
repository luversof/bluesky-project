package net.luversof.web.gate.config;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gate")
public record GateFeignProperties(FeignClient feignClient) {
	
	public static record FeignClient(Map<String, String> url) {}

}
