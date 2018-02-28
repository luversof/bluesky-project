package net.luversof.core;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@FeignClient("bluesky-ribbon-client")
public interface FeignTestService {

	@RequestMapping(value = "/_check/discoveryClientServiceList", method = RequestMethod.GET)
	String test();
		
	
}
