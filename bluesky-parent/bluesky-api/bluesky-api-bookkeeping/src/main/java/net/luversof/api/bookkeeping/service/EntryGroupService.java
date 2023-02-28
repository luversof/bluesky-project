package net.luversof.api.bookkeeping.service;

import java.util.List;
import java.util.Optional;

import net.luversof.api.bookkeeping.domain.EntryGroup;

public interface EntryGroupService {

	List<EntryGroup> createInitialData(String bookkeepingId);
	
	EntryGroup create(EntryGroup entryGroup);
	
	Optional<EntryGroup> findByEntryGroupId(String entryGroupId);
	
	List<EntryGroup> findByBookkeepingId(String bookkeepingId);
	
	EntryGroup update(EntryGroup entryGroup);
	
	void delete(EntryGroup entryGroup);
	
	void deleteAllBybookkeepingId(String bookkeepingId);
}
