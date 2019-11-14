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
	public List<Entry> searchUserEntry(EntryRequestParam entryRequestParam) {
		Bookkeeping targetBookkeeping = bookkeepingService.getUserBookkeeping(entryRequestParam.getBookkeeping().getUserId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		return entryRepository.findByBookkeepingIdAndEntryDateBetween(targetBookkeeping.getId(), entryRequestParam.getStartZonedDateTime(), entryRequestParam.getEndZonedDateTime());
	}

	public Entry updateUserEntry(Entry entry) {
		Bookkeeping bookkeeping = bookkeepingService.getUserBookkeeping(entry.getBookkeeping().getUserId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		Entry targetEntry = entryRepository.findById(entry.getId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_ENTRY));
		if (!targetEntry.getBookkeeping().getUserId().equals(bookkeeping.getUserId())) {
			throw new BlueskyException(BookkeepingErrorCode.NOT_OWNER_ENTRY);
		}
		
		targetEntry.setAmount(entry.getAmount());
		targetEntry.setEntryDate(entry.getEntryDate());
		targetEntry.setMemo(entry.getMemo());
		// TOOD 변경 할 속성 처리 - 하위 domain 에 대해 변경은 각 domain의 validation 체크가 필요함 
		
		return entryRepository.save(targetEntry);
	}

	public void deleteUserEntry(Entry entry) {
		Bookkeeping bookkeeping = bookkeepingService.getUserBookkeeping(entry.getBookkeeping().getUserId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		Entry targetEntry = entryRepository.findById(entry.getId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_ENTRY));
		if (!targetEntry.getBookkeeping().getUserId().equals(bookkeeping.getUserId())) {
			throw new BlueskyException(BookkeepingErrorCode.NOT_OWNER_ENTRY);
		}
		entryRepository.delete(targetEntry);
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
