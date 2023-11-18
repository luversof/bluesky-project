package net.luversof.web.gate.feign.bookkeeping.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import net.luversof.web.gate.feign.bookkeeping.domain.EntryGroup;


@FeignClient(value = "bluesky-api-bookkeeping", contextId = "api-bookkeeping-entryGroup", path = "/api/bookkeeping/entryGroup", url = "${gate.feign-client.url.bookkeeping:}")
public interface EntryGroupClient {

	@PostMapping
	EntryGroup create(@RequestBody EntryGroup entryGroup);

	@GetMapping
	List<EntryGroup> findByBookkeepingId(@RequestParam String bookkeepingId);

	@PutMapping
	EntryGroup update(@RequestBody EntryGroup entryGroup);

	@DeleteMapping
	void delete(@RequestBody EntryGroup entryGroup);
	
}