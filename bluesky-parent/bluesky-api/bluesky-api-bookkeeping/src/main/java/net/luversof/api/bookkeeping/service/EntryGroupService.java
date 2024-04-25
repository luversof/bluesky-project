//package net.luversof.api.bookkeeping.service;
//
//import java.util.List;
//import java.util.Optional;
//import java.util.UUID;
//
//import net.luversof.api.bookkeeping.domain.EntryGroup;
//
//public interface EntryGroupService {
//
//	List<EntryGroup> createInitialData(UUID bookkeepingId);
//	
//	EntryGroup create(EntryGroup entryGroup);
//	
//	Optional<EntryGroup> findByEntryGroupId(UUID entryGroupId);
//	
//	List<EntryGroup> findByBookkeepingId(UUID bookkeepingId);
//	
//	EntryGroup update(EntryGroup entryGroup);
//	
//	void delete(EntryGroup entryGroup);
//	
//	void deleteAllBybookkeepingId(UUID bookkeepingId);
//}
