package net.luversof.api.bookkeeping.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.Getter;
import net.luversof.api.bookkeeping.domain.Account;
import net.luversof.api.bookkeeping.repository.AccountRepository;

@Service
public class AccountService extends AbstractCommonService<Account, UUID> {

	@Getter
	private final AccountRepository repository;

	public AccountService(AccountRepository repository) {
		this.repository = repository;
	}
	
}
