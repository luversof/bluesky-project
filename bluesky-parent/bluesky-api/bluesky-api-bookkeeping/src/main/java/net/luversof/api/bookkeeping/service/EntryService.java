package net.luversof.api.bookkeeping.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.Getter;
import lombok.Setter;
import net.luversof.api.bookkeeping.domain.Entry;
import net.luversof.api.bookkeeping.repository.mariadb.EntryRepository;

@Service
public class EntryService  implements BasicCrudService<Entry, UUID> {

	@Getter
	@Setter(onMethod_ = @Autowired)
	private EntryRepository repository;

	public List<Entry> findByIncomeAssetId(UUID incomeAssetId) {
		return repository.findByIncomeAssetId(incomeAssetId);
	}
}
