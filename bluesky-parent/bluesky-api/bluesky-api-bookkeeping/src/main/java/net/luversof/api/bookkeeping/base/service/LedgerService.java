package net.luversof.api.bookkeeping.base.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.luversof.api.bookkeeping.base.domain.Ledger;
import net.luversof.api.bookkeeping.base.repository.mariadb.LedgerRepository;

@RequiredArgsConstructor
@Service
public class LedgerService extends AbstractBaseService<Ledger, UUID> {
	
	@Getter
	private final LedgerRepository repository;

	public List<Ledger> findByUserId(UUID userId) {
		return repository.findByUserId(userId);
	}

}
