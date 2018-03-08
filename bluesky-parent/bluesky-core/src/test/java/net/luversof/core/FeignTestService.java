package net.luversof.core;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient("bluesky-web-default")
public interface FeignTestService {

	@GetMapping("/_check/discoveryClientServiceList")
	String test();
	
}
