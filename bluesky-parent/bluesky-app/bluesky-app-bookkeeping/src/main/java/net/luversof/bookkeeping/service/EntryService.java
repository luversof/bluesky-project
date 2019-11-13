package net.luversof.bookkeeping.service;


import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.domain.Entry;
import net.luversof.bookkeeping.domain.web.EntryRequestParam;
import net.luversof.bookkeeping.repository.EntryRepository;
import net.luversof.boot.exception.BlueskyException;

@Service
public class EntryService {
	
	@Autowired
	private EntryRepository entryRepository;
	
	@Autowired
	private BookkeepingService bookkeepingService;

	public Entry createUserEntry(Entry entry) {
		Bookkeeping bookkeeping = bookkeepingService.getUserBookkeeping(entry.getBookkeeping().getUserId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		entry.setBookkeeping(bookkeeping);
		return entryRepository.save(entry);
	}

	/**
	 * 기간 기준 검색
	 * @param entrySearchInfo
	 * @return
	 */
	public List<Entry> searchUserEntryByEntryRequestParam(EntryRequestParam entryRequestParam) {
		Bookkeeping targetBookkeeping = bookkeepingService.getUserBookkeeping(entryRequestParam.getBookkeeping().getUserId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		return entryRepository.findByBookkeepingIdAndEntryDateBetween(targetBookkeeping.getId(), entryRequestParam.getStartZonedDateTime(), entryRequestParam.getEndZonedDateTime());
	}

	public Entry findOne(long id) {
		return entryRepository.getOne(id);
	}
	
	public Entry updateUserEntry(Entry entry) {
		Bookkeeping bookkeeping = bookkeepingService.getUserBookkeeping(entry.getBookkeeping().getUserId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		entry.setBookkeeping(bookkeeping);
		Entry targetEntry = findOne(entry.getId());
		if (!targetEntry.getBookkeeping().getUserId().equals(entry.getBookkeeping().getUserId())) {
			throw new BlueskyException(BookkeepingErrorCode.NOT_OWNER_ENTRY);
		}
		return entryRepository.save(entry);
	}

	public void deleteUserEntry(Entry entry) {
		Bookkeeping bookkeeping = bookkeepingService.getUserBookkeeping(entry.getBookkeeping().getUserId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		entry.setBookkeeping(bookkeeping);
		Entry targetEntry = findOne(entry.getId());
		if (!targetEntry.getBookkeeping().getUserId().equals(entry.getBookkeeping().getUserId())) {
			throw new BlueskyException(BookkeepingErrorCode.NOT_OWNER_ENTRY);
		}
		entryRepository.delete(entry);
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
