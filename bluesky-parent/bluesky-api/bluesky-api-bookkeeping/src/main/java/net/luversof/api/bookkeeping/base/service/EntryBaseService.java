package net.luversof.api.bookkeeping.base.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.luversof.api.bookkeeping.base.domain.Entry;
import net.luversof.api.bookkeeping.base.repository.mariadb.EntryRepository;

@RequiredArgsConstructor
@Service
public class EntryBaseService  extends AbstractBaseService<Entry, UUID> {


	@Getter
	private final EntryRepository repository;

	public List<Entry> findByBookkeepingId(UUID accountId) {
		return repository.findByAccountId(accountId);
	}
}
