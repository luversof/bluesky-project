package net.luversof.bookkeeping.service;

import java.time.LocalDate;
import java.util.List;

import net.luversof.bookkeeping.domain.Entry;
import net.luversof.bookkeeping.domain.web.EntryRequestParam;

public interface EntryService {

	Entry create(Entry entry);
	
	List<Entry> search(EntryRequestParam entryRequestParam);
	
	/**
	 * test용 메소드
	 * @param bookkeepingId
	 * @param startZonedDateTime
	 * @param endZonedDateTime
	 * @return
	 */
	List<Entry> findByBookkeepingIdAndEntryDateBetween(String bookkeepingId, LocalDate startLocalDate, LocalDate endLocalDate);
	
	Entry update(Entry entry);
	
	void delete(Entry entry);
	
	void deleteByBookkeepingId(String bookkeepingId);
}
