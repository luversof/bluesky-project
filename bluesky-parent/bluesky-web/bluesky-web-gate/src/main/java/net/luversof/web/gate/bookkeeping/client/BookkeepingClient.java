package net.luversof.web.gate.bookkeeping.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import net.luversof.web.gate.bookkeeping.domain.Bookkeeping;

@FeignClient(value = "bluesky-api-bookkeeping", contextId = "api-bookkeeping", path = "/api/bookkeeping", url = "${gate.feign-client.url.bookkeeping:}")
public interface BookkeepingClient {

	@PostMapping
	Bookkeeping create(@RequestBody Bookkeeping bookkeeping);
	
	@GetMapping
	List<Bookkeeping> findByUserId(@RequestParam String userId);
	
	@PutMapping
	Bookkeeping update(@RequestBody Bookkeeping bookkeeping);
	
	@DeleteMapping
	void delete(@RequestBody Bookkeeping bookkeeping);
	
}
