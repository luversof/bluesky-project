package net.luversof.bookkeeping.service;

import static net.luversof.bookkeeping.BookkeepingConstants.BOOKKEEPING_TRANSACTIONMANAGER;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.bookkeeping.BookkeepingErrorCode;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.domain.Entry;
import net.luversof.bookkeeping.domain.EntrySearchInfo;
import net.luversof.bookkeeping.repository.EntryRepository;
import net.luversof.bookkeeping.util.BookkeepingUtils;
import net.luversof.core.exception.BlueskyException;

@Service
@Transactional(BOOKKEEPING_TRANSACTIONMANAGER)
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

	@Transactional(value = BOOKKEEPING_TRANSACTIONMANAGER, readOnly = true)
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
	 * 넘어온 startDate, endDate 사이의 entryList를 반환.
	 * 두 매개변수중 하나라도 null인 경우 bookkeeping의 기준일에 해당하는 현재 월의 데이터를 반환함
	 * @param entrySearchInfo
	 * @return
	 */
	@Transactional(value = BOOKKEEPING_TRANSACTIONMANAGER, readOnly = true)
	public List<Entry> findByEntrySearchInfo(EntrySearchInfo entrySearchInfo) {
		Bookkeeping targetBookkeeping = bookkeepingService.findOne(entrySearchInfo.getBookkeepingId());
		int baseDate = targetBookkeeping.getBaseDate();
		LocalDateTime startDateTime = BookkeepingUtils.getStartDateTime(entrySearchInfo.getTargetDate(), baseDate);
		LocalDateTime endDateTime = BookkeepingUtils.getEndDateTime(entrySearchInfo.getTargetDate(), baseDate);
		return entryRepository.findByBookkeepingIdAndEntryDateBetween(entrySearchInfo.getBookkeepingId(), startDateTime, endDateTime);
	}
}
