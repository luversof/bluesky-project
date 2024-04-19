package net.luversof.api.bookkeeping.base.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.luversof.api.bookkeeping.base.domain.Account;
import net.luversof.api.bookkeeping.base.repository.AccountRepository;

@RequiredArgsConstructor
@Service
public class AccountBaseService extends AbstractBaseService<Account, UUID> {

	@Getter
	private final AccountRepository repository;
	
}
