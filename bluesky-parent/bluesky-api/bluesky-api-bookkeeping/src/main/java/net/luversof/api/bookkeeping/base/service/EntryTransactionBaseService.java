package net.luversof.api.bookkeeping.base.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.luversof.api.bookkeeping.base.domain.EntryTransaction;
import net.luversof.api.bookkeeping.base.repository.mariadb.EntryTransactionRepository;

@RequiredArgsConstructor
@Service
public class EntryTransactionBaseService extends AbstractBaseService<EntryTransaction, UUID> {

	@Getter
	private final EntryTransactionRepository repository;

	public List<EntryTransaction> findByBookkeepingId(UUID bookkeepingId) {
		return repository.findByBookkeepingId(bookkeepingId);
	}

}
