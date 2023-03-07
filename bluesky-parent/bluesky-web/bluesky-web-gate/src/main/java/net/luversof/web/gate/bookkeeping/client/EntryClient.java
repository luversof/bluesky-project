package net.luversof.web.gate.bookkeeping.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import net.luversof.web.gate.bookkeeping.domain.Entry;
import net.luversof.web.gate.bookkeeping.domain.EntryRequestParam;


@FeignClient(value = "bluesky-api-bookkeeping", contextId = "api-bookkeeping-entry", path = "/api/bookkeeping/entry", url = "${gate.feign-client.url.bookkeeping:}")
public interface EntryClient {

	@PostMapping
	Entry create(@RequestBody Entry entry);
	
	@GetMapping
	List<Entry> search(EntryRequestParam entryRequestParam);
	
	@PutMapping
	Entry update(@RequestBody Entry entry);
	
	@DeleteMapping
	void delete(Entry entry);
}
