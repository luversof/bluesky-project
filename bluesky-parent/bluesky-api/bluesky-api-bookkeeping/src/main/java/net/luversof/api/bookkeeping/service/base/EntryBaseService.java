package net.luversof.api.bookkeeping.service.base;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.Getter;
import lombok.Setter;
import net.luversof.api.bookkeeping.domain.Entry;
import net.luversof.api.bookkeeping.repository.mariadb.EntryRepository;

@Service
public class EntryBaseService implements BaseService<Entry, UUID> {

	@Getter
	@Setter(onMethod_ = @Autowired)
	private EntryRepository repository;
	
	public List<Entry> findByBookkeepingId(UUID bookkeepingId) {
		return repository.findByBookkeepingId(bookkeepingId);
	}

	public List<Entry> findByIncomeAssetId(UUID incomeAssetId) {
		return repository.findByIncomeAssetId(incomeAssetId);
	}
	
	public List<Entry> findByOutgoingAssetId(UUID outgoingAssetId) {
		return repository.findByOutgoingAssetId(outgoingAssetId);
	}

}
