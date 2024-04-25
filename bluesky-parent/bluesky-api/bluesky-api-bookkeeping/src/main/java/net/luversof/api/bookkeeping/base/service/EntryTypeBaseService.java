package net.luversof.api.bookkeeping.base.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.luversof.api.bookkeeping.base.domain.EntryType;
import net.luversof.api.bookkeeping.base.repository.mariadb.EntryTypeRepository;

@RequiredArgsConstructor
@Service
public class EntryTypeBaseService extends AbstractBaseService<EntryType, UUID> {

	@Getter
	private final EntryTypeRepository repository;

	public List<EntryType> findByBookkeepingId(UUID bookkeepingId) {
		return repository.findByBookkeepingId(bookkeepingId);
	}

}
