package net.luversof.api.bookkeeping.repository.mariadb;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import net.luversof.api.bookkeeping.domain.Entry;

public interface EntryRepository extends JpaRepository<Entry, UUID> {
	
	List<Entry> findByIncomeAssetId(UUID incomeAssetId);
	List<Entry> findByOutgoingAssetId(UUID outgoingAssetId);
	
	long deleteByBookkeepingId(UUID bookkeepingId);
}
