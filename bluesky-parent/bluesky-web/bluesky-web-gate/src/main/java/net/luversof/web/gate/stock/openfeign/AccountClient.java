package net.luversof.web.gate.stock.openfeign;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import net.luversof.web.gate.stock.domain.Account;

@FeignClient(name = "bluesky-api-stock", contextId="api-stock-account", path = "/api/account", url = "${gate.feign-client.url.stock:}")
public interface AccountClient {

	@PostMapping
	Account createAccount(@RequestBody Account account);
	
	@GetMapping("/{id}")
	Optional<Account> getAccountById(@PathVariable UUID id);
	
	@GetMapping("/search/findByUserId/{userId}")
	List<Account> getAccountsByUserId(@PathVariable UUID userId);

}
