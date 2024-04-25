package net.luversof.api.bookkeeping.base.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.luversof.api.bookkeeping.base.domain.AccountType;
import net.luversof.api.bookkeeping.base.repository.mariadb.AccountTypeRepository;

@RequiredArgsConstructor
@Service
public class AccountTypeBaseService extends AbstractBaseService<AccountType, UUID> {

	@Getter
	private final AccountTypeRepository repository;
	
	public List<AccountType> findByBookkeepingId(UUID bookkeepingId) {
		return repository.findByBookkeepingId(bookkeepingId);
	}

}
