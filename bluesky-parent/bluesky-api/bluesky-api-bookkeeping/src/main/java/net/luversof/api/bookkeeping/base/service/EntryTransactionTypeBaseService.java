package net.luversof.api.bookkeeping.base.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.luversof.api.bookkeeping.base.domain.EntryTransactionType;
import net.luversof.api.bookkeeping.base.repository.mariadb.EntryTransactionTypeRepository;

@RequiredArgsConstructor
@Service
public class EntryTransactionTypeBaseService extends AbstractBaseService<EntryTransactionType, UUID> {

	@Getter
	private final EntryTransactionTypeRepository repository;

	public List<EntryTransactionType> findByBookkeepingId(UUID bookkeepingId) {
		return repository.findByBookkeepingId(bookkeepingId);
	}

}
