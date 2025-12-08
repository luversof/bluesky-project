package net.luversof.web.gate.bookkeeping.httpexchange;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

import net.luversof.web.gate.bookkeeping.domain.Entry;
import net.luversof.web.gate.bookkeeping.domain.EntryRequestParam;


@HttpExchange(url = "/api/bookkeeping/entry", contentType = MediaType.APPLICATION_JSON_VALUE)
public interface EntryClient {

	@PostExchange
	Entry create(@RequestBody Entry entry);
	
	@GetExchange
	List<Entry> search(EntryRequestParam entryRequestParam);
	
	@PutExchange
	Entry update(@RequestBody Entry entry);
	
	@DeleteExchange
	void delete(Entry entry);
}