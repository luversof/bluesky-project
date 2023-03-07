package net.luversof.web.gate.bookkeeping.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import net.luversof.web.gate.bookkeeping.domain.Asset;


@FeignClient(value = "bluesky-api-bookkeeping", contextId = "api-bookkeeping-asset", path = "/api/bookkeeping/asset", url = "${gate.feign-client.url.bookkeeping:}")
public interface AssetClient {

	@PostMapping
	Asset create(@RequestBody Asset asset);
	
	@GetMapping
	List<Asset> findByBookkeepingId(@RequestParam String bookkeepingId);

	@PutMapping
	Asset update(@RequestBody Asset asset);

	@DeleteMapping
	void delete(@RequestBody Asset asset);

}