package net.luversof.api.bookkeeping.service;

import java.time.LocalDate;
import java.util.List;

import io.github.luversof.boot.autoconfigure.validation.annotation.BlueskyValidated;
import net.luversof.api.bookkeeping.domain.Entry;
import net.luversof.api.bookkeeping.domain.web.EntryRequestParam;

public interface EntryService {

	Entry create(@BlueskyValidated(Entry.Create.class) Entry entry);
	
	List<Entry> search(EntryRequestParam entryRequestParam);
	
	/**
	 * test용 메소드
	 * @param bookkeepingId
	 * @param startZonedDateTime
	 * @param endZonedDateTime
	 * @return
	 */
	List<Entry> findByBookkeepingIdAndEntryDateBetween(String bookkeepingId, LocalDate startLocalDate, LocalDate endLocalDate);
	
	Entry update(@BlueskyValidated(Entry.Update.class) Entry entry);
	
	void delete(@BlueskyValidated(Entry.Delete.class) Entry entry);
	
	void deleteByBookkeepingId(String bookkeepingId);
}
