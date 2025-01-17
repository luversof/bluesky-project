package net.luversof.web.gate.bookkeeping.openfeign;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import net.luversof.web.gate.bookkeeping.domain.AssetType;


@FeignClient(value = "bluesky-api-bookkeeping", contextId = "api-bookkeeping-assetGroup", path = "/api/bookkeeping/assetGroup", url = "${gate.feign-client.url.bookkeeping:}")
public interface AssetGroupClient {

	@PostMapping
	AssetType create(@RequestBody AssetType assetGroup);
	
	@GetMapping
	List<AssetType> findByBookkeepingId(@RequestParam String bookkeepingId);
	
	@PutMapping
	AssetType update(@RequestBody AssetType assetGroup);
	
	@DeleteMapping
	void delete(@RequestBody AssetType assetGroup);

}