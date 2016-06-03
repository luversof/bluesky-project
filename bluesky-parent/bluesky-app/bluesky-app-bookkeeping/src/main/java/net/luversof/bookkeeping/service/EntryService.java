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
import net.luversof.bookkeeping.domain.Statistics;
import net.luversof.bookkeeping.domain.StatisticsSearchInfo;
import net.luversof.bookkeeping.mapper.StatisticsMapper;
import net.luversof.bookkeeping.repository.EntryRepository;
import net.luversof.core.exception.BlueskyException;

@Service
@Transactional(BOOKKEEPING_TRANSACTIONMANAGER)
public class EntryService {
	
	@Autowired
	private EntryRepository entryRepository;
	
	@Autowired
	private BookkeepingService bookkeepingService;
	
	@Autowired
	private StatisticsMapper statisticsMapper;

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
	 * @param entrySearchInfo
	 * @return
	 */
	@Transactional(value = BOOKKEEPING_TRANSACTIONMANAGER, readOnly = true)
	public List<Entry> findByEntrySearchInfo(EntrySearchInfo entrySearchInfo) {
		Bookkeeping targetBookkeeping = bookkeepingService.findOne(entrySearchInfo.getBookkeeping().getId());
		int baseDate = targetBookkeeping.getBaseDate();
		entrySearchInfo.setBaseDate(baseDate);
		return entryRepository.findByBookkeepingIdAndEntryDateBetween(entrySearchInfo.getBookkeeping().getId(), entrySearchInfo.getStartLocalDateTime(), entrySearchInfo.getEndLocalDateTime());
	}
	
	/**
	 * @param entrySearchInfo
	 * @return
	 */
	@Transactional(value = BOOKKEEPING_TRANSACTIONMANAGER, readOnly = true)
	public List<Entry> findByStatisticsSearchInfo(StatisticsSearchInfo statisticsSearchInfo) {
		Bookkeeping targetBookkeeping = bookkeepingService.findOne(statisticsSearchInfo.getBookkeeping().getId());
		int baseDate = targetBookkeeping.getBaseDate();
		statisticsSearchInfo.setBaseDate(baseDate);
		return entryRepository.findByBookkeepingIdAndEntryDateBetween(statisticsSearchInfo.getBookkeeping().getId(), statisticsSearchInfo.getStartLocalDateTime(), statisticsSearchInfo.getEndLocalDateTime());
	}
	
	/**
	 * test용 메소드
	 * @param bookkeepingId
	 * @param startLocalDateTime
	 * @param endLocalDateTime
	 * @return
	 */
	@Transactional(value = BOOKKEEPING_TRANSACTIONMANAGER, readOnly = true)
	public List<Entry> findByBookkeepingIdAndEntryDateBetween(long bookkeepingId, LocalDateTime startLocalDateTime, LocalDateTime endLocalDateTime) {
		return entryRepository.findByBookkeepingIdAndEntryDateBetween(bookkeepingId, startLocalDateTime, endLocalDateTime);
	}
	
	/**
	 * test용 메소드
	 * @return
	 */
	@Transactional(value = BOOKKEEPING_TRANSACTIONMANAGER, readOnly = true)
	public List<Statistics> test() {
		return statisticsMapper.selectStatistics();
	}
	
	
}
