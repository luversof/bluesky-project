package net.luversof.api.bookkeeping.service.base;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.Getter;
import lombok.Setter;
import net.luversof.api.bookkeeping.domain.EntryType;
import net.luversof.api.bookkeeping.repository.mariadb.EntryTypeRepository;

@Service
public class EntryTypeBaseService implements BaseService<EntryType, UUID> {

	@Getter
	@Setter(onMethod_ = @Autowired)
	private EntryTypeRepository repository;

	public List<EntryType> findByBookkeepingId(UUID bookkeepingId) {
		return repository.findByBookkeepingId(bookkeepingId);
	}

}
