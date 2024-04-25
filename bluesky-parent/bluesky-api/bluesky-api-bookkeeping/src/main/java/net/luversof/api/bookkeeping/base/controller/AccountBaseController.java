package net.luversof.api.bookkeeping.base.controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import net.luversof.api.bookkeeping.base.domain.Account;
import net.luversof.api.bookkeeping.base.service.AccountBaseService;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/api/account/base", produces = MediaType.APPLICATION_JSON_VALUE)
public class AccountBaseController {

	private final AccountBaseService accountBaseService;
	
	@PutMapping
	public Account update(@RequestBody @Validated(Account.Update.class) Account account) {
		return accountBaseService.update(account);
	}
	
	@GetMapping("/{id}")
	public Optional<Account> findById(@PathVariable UUID id) {
		return accountBaseService.findById(id);
	}
	
	@GetMapping("/search/findByBookkeepingId/{bookkeepingId}")
	public List<Account> findByBookkeepingId(@PathVariable UUID bookkeepingId) {
		return accountBaseService.findByBookkeepingId(bookkeepingId);
	}

}
