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
import net.luversof.api.bookkeeping.base.domain.AccountType;
import net.luversof.api.bookkeeping.base.service.AccountTypeBaseService;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/api/accountType/base", produces = MediaType.APPLICATION_JSON_VALUE)
public class AccountTypeBaseController {

	private final AccountTypeBaseService accountTypeBaseService;
	
	@PutMapping
	public AccountType update(@RequestBody @Validated(AccountType.Update.class) AccountType accountType) {
		return accountTypeBaseService.update(accountType);
	}
	
	@GetMapping("/{id}")
	public Optional<AccountType> findById(@PathVariable UUID id) {
		return accountTypeBaseService.findById(id);
	}
	
	@GetMapping("/search/findByBookkeepingId/{bookkeepingId}")
	public List<AccountType> findByBookkeepingId(@PathVariable UUID bookkeepingId) {
		return accountTypeBaseService.findByBookkeepingId(bookkeepingId);
	}

}