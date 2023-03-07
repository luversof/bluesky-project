package net.luversof.web.gate.bookkeeping.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import net.luversof.web.gate.bookkeeping.domain.AssetGroup;


@FeignClient(value = "bluesky-api-bookkeeping", contextId = "api-bookkeeping-assetGroup", path = "/api/bookkeeping/assetGroup", url = "${gate.feign-client.url.bookkeeping:}")
public interface AssetGroupClient {

	@PostMapping
	AssetGroup create(@RequestBody AssetGroup assetGroup);
	
	@GetMapping
	List<AssetGroup> findByBookkeepingId(@RequestParam String bookkeepingId);
	
	@PutMapping
	AssetGroup update(@RequestBody AssetGroup assetGroup);
	
	@DeleteMapping
	void delete(@RequestBody AssetGroup assetGroup);

}