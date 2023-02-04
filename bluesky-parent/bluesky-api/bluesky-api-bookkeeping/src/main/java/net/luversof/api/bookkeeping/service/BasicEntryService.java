package net.luversof.api.bookkeeping.service;


import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.api.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.api.bookkeeping.constant.EntryGroupType;
import net.luversof.api.bookkeeping.domain.Bookkeeping;
import net.luversof.api.bookkeeping.domain.Entry;
import net.luversof.api.bookkeeping.domain.web.EntryRequestParam;
import net.luversof.api.bookkeeping.repository.EntryRepository;

@Service
public class BasicEntryService implements EntryService {
	
	@Autowired
	private EntryRepository entryRepository;
	
	@Autowired
	private BasicAssetService assetService;
	
	@Autowired
	private BasicBookkeepingService bookkeepingService;

	@Override
	public Entry create(Entry entry) {
		// 기록 유형 확인
		EntryGroupType entryGroupType = null;
		if (StringUtils.hasText(entry.getIncomeAssetId()) && StringUtils.hasText(entry.getExpenseAssetId())) {
			entryGroupType = EntryGroupType.TRANSFER;
		} else if (StringUtils.hasText(entry.getIncomeAssetId())) {
			entryGroupType = EntryGroupType.INCOME;
		} else if (StringUtils.hasText(entry.getExpenseAssetId())) {
			entryGroupType = EntryGroupType.EXPENSE;
		} else {
			throw new BlueskyException(BookkeepingErrorCode.NOT_EXIST_ENTRYGROUPTYPE);
		}
		entry.setEntryGroupType(entryGroupType);
		
		// incomeAsset, expenseAsset이 해당 유저의 정보인지 확인
		if (entryGroupType == EntryGroupType.INCOME || entryGroupType == EntryGroupType.TRANSFER) {
			assetService.findByAssetId(entry.getIncomeAssetId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_INCOME_ASSET));
		}
		if (entryGroupType == EntryGroupType.INCOME || entryGroupType == EntryGroupType.TRANSFER) {
			assetService.findByAssetId(entry.getExpenseAssetId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_EXPENSE_ASSET));
		}
		return entryRepository.save(entry);
	}

	@Override
	public List<Entry> search(EntryRequestParam entryRequestParam) {
		Bookkeeping targetBookkeeping = bookkeepingService.findByBookkeepingId(entryRequestParam.getBookkeepingId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		return entryRepository.findByBookkeepingIdAndEntryDateBetween(targetBookkeeping.getBookkeepingId(), entryRequestParam.getStartLocalDate(), entryRequestParam.getEndLocalDate());
	}
	
	@Override
	public List<Entry> findByBookkeepingIdAndEntryDateBetween(String bookkeepingId, LocalDate startLocalDate, LocalDate endLocalDate) {
		return entryRepository.findByBookkeepingIdAndEntryDateBetween(bookkeepingId, startLocalDate, endLocalDate);
	}

	@Override
	public Entry update(Entry entry) {
		bookkeepingService.findByBookkeepingId(entry.getBookkeepingId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		Entry targetEntry = entryRepository.findByEntryId(entry.getEntryId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_ENTRY));
		
		targetEntry.setAmount(entry.getAmount());
		targetEntry.setEntryDate(entry.getEntryDate());
		targetEntry.setMemo(entry.getMemo());
		// TOOD 변경 할 속성 처리 - 하위 domain 에 대해 변경은 각 domain의 validation 체크가 필요함 
		
		return entryRepository.save(targetEntry);
	}

	@Override
	public void delete(Entry entry) {
		bookkeepingService.findByBookkeepingId(entry.getBookkeepingId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		Entry targetEntry = entryRepository.findByEntryId(entry.getEntryId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_ENTRY));
		entryRepository.delete(targetEntry);
	}

	
	@Override
	public void deleteByBookkeepingId(String bookkeepingId) {
		entryRepository.deleteByBookkeepingId(bookkeepingId);
	}
	
}
