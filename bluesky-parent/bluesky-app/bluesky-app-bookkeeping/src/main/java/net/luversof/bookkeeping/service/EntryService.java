package net.luversof.bookkeeping.service;


import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.domain.Entry;
import net.luversof.bookkeeping.domain.EntrySearchInfo;
import net.luversof.bookkeeping.repository.EntryRepository;
import net.luversof.boot.exception.BlueskyException;

@Service
public class EntryService {
	
	@Autowired
	private EntryRepository entryRepository;
	
	@Autowired
	private BookkeepingService bookkeepingService;

	public Entry create(Entry entry) {
		Bookkeeping bookkeeping = bookkeepingService.getUserBookkeeping(entry.getBookkeeping()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		entry.setBookkeeping(bookkeeping);
		return entryRepository.save(entry);
	}
	
	public Entry update(Entry entry) {
		Bookkeeping bookkeeping = bookkeepingService.getUserBookkeeping(entry.getBookkeeping()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		entry.setBookkeeping(bookkeeping);
		Entry targetEntry = findOne(entry.getId());
		if (!targetEntry.getBookkeeping().getUserId().equals(entry.getBookkeeping().getUserId())) {
			throw new BlueskyException(BookkeepingErrorCode.NOT_OWNER_ENTRY);
		}
		return entryRepository.save(entry);
	}

	public Entry findOne(long id) {
		return entryRepository.getOne(id);
	}

	public void delete(Entry entry) {
		Bookkeeping bookkeeping = bookkeepingService.getUserBookkeeping(entry.getBookkeeping()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		entry.setBookkeeping(bookkeeping);
		Entry targetEntry = findOne(entry.getId());
		if (!targetEntry.getBookkeeping().getUserId().equals(entry.getBookkeeping().getUserId())) {
			throw new BlueskyException(BookkeepingErrorCode.NOT_OWNER_ENTRY);
		}
		entryRepository.delete(entry);
	}
	
	/**
	 * @param entrySearchInfo
	 * @return
	 */
	public List<Entry> findByEntrySearchInfo(EntrySearchInfo entrySearchInfo) {
		Bookkeeping targetBookkeeping = bookkeepingService.getUserBookkeeping(entrySearchInfo.getBookkeeping()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
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
	public List<Entry> findByBookkeepingIdAndEntryDateBetween(UUID bookkeepingId, ZonedDateTime startZonedDateTime, ZonedDateTime endZonedDateTime) {
		return entryRepository.findByBookkeepingIdAndEntryDateBetween(bookkeepingId, startZonedDateTime, endZonedDateTime);
	}
	
	
	public int deleteByBookkeepingId(UUID bookkeepingId) {
		return entryRepository.deleteByBookkeepingIdQuery(bookkeepingId);
	}
	
}
