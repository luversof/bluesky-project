package net.luversof.bookkeeping.service;


import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.bookkeeping.constant.EntryGroupType;
import net.luversof.bookkeeping.domain.Asset;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.domain.Entry;
import net.luversof.bookkeeping.domain.EntryGroup;
import net.luversof.bookkeeping.domain.web.EntryRequestParam;
import net.luversof.bookkeeping.repository.EntryRepository;
import net.luversof.boot.exception.BlueskyException;

@Service
public class EntryService {
	
	@Autowired
	private EntryRepository entryRepository;
	
	@Autowired
	private BookkeepingService bookkeepingService;
	
	@Autowired
	private EntryGroupService entryGroupService;
	
	@Autowired
	private AssetService assetService;

	public Entry createUserEntry(Entry entry) {
		Bookkeeping bookkeeping = bookkeepingService.getUserBookkeeping(entry.getBookkeeping().getUserId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		entry.setBookkeeping(bookkeeping);
		
		// 요청이 income인 경우 expenseAsset 삭제 처리
		if (entry.getEntryGroupType() == EntryGroupType.INCOME) {
			entry.setExpenseAsset(null);
		}
		
		// 요청이 expense인 경우 incomeAsset 삭제 처리
		if (entry.getEntryGroupType() == EntryGroupType.EXPENSE) {
			entry.setIncomeAsset(null);
		}
		
		// incomeAsset, expenseAsset이 해당 유저의 정보인지 확인
		if (entry.getIncomeAsset() != null) {
			
			Asset targetAsset = assetService.findById(entry.getIncomeAsset().getId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_ASSET));
			if (!targetAsset.getBookkeeping().getUserId().equals(entry.getBookkeeping().getUserId())) {
				throw new BlueskyException(BookkeepingErrorCode.NOT_OWNER_ASSET);
			}
			entry.setIncomeAsset(targetAsset);
		}
		
		
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
