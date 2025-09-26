package net.luversof.api.bookkeeping.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import net.luversof.api.bookkeeping.domain.Entry;

public interface EntryRepository extends CrudRepository<Entry, UUID> {
	
	List<Entry> findByBookkeepingId(UUID bookkeepingId);
	
	/** (s) test **/
	List<Entry> findByIncomeAssetId(UUID incomeAssetId);
	List<Entry> findByOutgoingAssetId(UUID outgoingAssetId);
	long deleteByBookkeepingId(UUID bookkeepingId);
	/** (e) test **/
}
