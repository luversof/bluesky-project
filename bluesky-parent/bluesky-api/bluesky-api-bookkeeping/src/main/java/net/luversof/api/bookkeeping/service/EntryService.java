//package net.luversof.api.bookkeeping.service;
//
//import java.time.ZonedDateTime;
//import java.util.List;
//import java.util.UUID;
//
//import net.luversof.api.bookkeeping.base.domain.Entry;
//import net.luversof.api.bookkeeping.domain.web.EntryRequestParam;
//
//public interface EntryService {
//
//	Entry create(Entry entry);
//	
//	List<Entry> search(EntryRequestParam entryRequestParam);
//	
//	/**
//	 * test용 메소드
//	 * @param bookkeepingId
//	 * @param startZonedDateTime
//	 * @param endZonedDateTime
//	 * @return
//	 */
//	List<Entry> findByBookkeepingIdAndEntryDateBetween(UUID bookkeepingId, ZonedDateTime startDate, ZonedDateTime endDate);
//	
//	Entry update(Entry entry);
//	
//	void delete(Entry entry);
//	
//	void deleteByBookkeepingId(UUID bookkeepingId);
//}
