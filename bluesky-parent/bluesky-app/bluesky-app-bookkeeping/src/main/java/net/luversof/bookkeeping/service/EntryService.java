package net.luversof.bookkeeping.service;


import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.domain.Entry;
import net.luversof.bookkeeping.domain.EntrySearchInfo;
import net.luversof.bookkeeping.exception.BookkeepingErrorCode;
import net.luversof.bookkeeping.repository.EntryRepository;
import net.luversof.core.exception.BlueskyException;

@Service
public class EntryService {
	
	@Autowired
	private EntryRepository entryRepository;
	
	@Autowired
	private BookkeepingService bookkeepingService;

	public Entry create(Entry entry) {
		Bookkeeping targetBookkeeping = bookkeepingService.findOne(entry.getBookkeeping().getId());
		if (targetBookkeeping.getUserId() != entry.getBookkeeping().getUserId()) {
			throw new BlueskyException(BookkeepingErrorCode.NOT_OWNER_BOOKKEEPING);
		}
		return entryRepository.save(entry);
	}
	
	public Entry update(Entry entry) {
		Bookkeeping targetBookkeeping = bookkeepingService.findOne(entry.getBookkeeping().getId());
		if (targetBookkeeping.getUserId() != entry.getBookkeeping().getUserId()) {
			throw new BlueskyException(BookkeepingErrorCode.NOT_OWNER_BOOKKEEPING);
		}
		Entry targetEntry = findOne(entry.getId());
		if (targetEntry.getBookkeeping().getUserId() != entry.getBookkeeping().getUserId()) {
			throw new BlueskyException(BookkeepingErrorCode.NOT_OWNER_ENTRY);
		}
		return entryRepository.save(entry);
	}

	public Entry findOne(long id) {
		return entryRepository.findOne(id);
	}

	public void delete(Entry entry) {
		Bookkeeping targetBookkeeping = bookkeepingService.findOne(entry.getBookkeeping().getId());
		if (targetBookkeeping.getUserId() != entry.getBookkeeping().getUserId()) {
			throw new BlueskyException(BookkeepingErrorCode.NOT_OWNER_BOOKKEEPING);
		}
		Entry targetEntry = findOne(entry.getId());
		if (targetEntry.getBookkeeping().getUserId() != entry.getBookkeeping().getUserId()) {
			throw new BlueskyException(BookkeepingErrorCode.NOT_OWNER_ENTRY);
		}
		entryRepository.delete(entry);
	}
	
	/**
	 * @param entrySearchInfo
	 * @return
	 */
	public List<Entry> findByEntrySearchInfo(EntrySearchInfo entrySearchInfo) {
		Bookkeeping targetBookkeeping = bookkeepingService.findOne(entrySearchInfo.getBookkeeping().getId());
		int baseDate = targetBookkeeping.getBaseDate();
		entrySearchInfo.setBaseDate(baseDate);
		return entryRepository.findByBookkeepingIdAndEntryDateBetween(entrySearchInfo.getBookkeeping().getId(), entrySearchInfo.getStartZonedDateTime(), entrySearchInfo.getEndZonedDateTime());
	}

	/**
	 * test용 메소드
	 * @param bookkeepingId
	 * @param startZonedDateTime
	 * @param endZonedDateTime
	 * @return
	 */
	public List<Entry> findByBookkeepingIdAndEntryDateBetween(long bookkeepingId, ZonedDateTime startZonedDateTime, ZonedDateTime endZonedDateTime) {
		return entryRepository.findByBookkeepingIdAndEntryDateBetween(bookkeepingId, startZonedDateTime, endZonedDateTime);
	}
	
	
	public void test(long id) {
		entryRepository.deleteById(id);
	}
	
}
