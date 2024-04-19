//package net.luversof.api.bookkeeping.service;
//
//
//import java.time.ZonedDateTime;
//import java.util.List;
//import java.util.UUID;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import io.github.luversof.boot.exception.BlueskyException;
//import net.luversof.api.bookkeeping.base.domain.Bookkeeping;
//import net.luversof.api.bookkeeping.base.domain.Entry;
//import net.luversof.api.bookkeeping.constant.BookkeepingErrorCode;
//import net.luversof.api.bookkeeping.constant.EntryGroupType;
//import net.luversof.api.bookkeeping.domain.web.EntryRequestParam;
//import net.luversof.api.bookkeeping.repository.EntryRepository;
//
//@Service
//public class BasicEntryService implements EntryService {
//	
//	@Autowired
//	private EntryRepository entryRepository;
//	
//	@Autowired
//	private BasicAssetService assetService;
//	
//	@Autowired
//	private BasicBookkeepingService bookkeepingService;
//
//	@Override
//	public Entry create(Entry entry) {
//		// 기록 유형 확인
//		EntryGroupType entryGroupType = null;
//		if (entry.getIncomeAssetId() != null && entry.getExpenseAssetId() != null) {
//			entryGroupType = EntryGroupType.TRANSFER;
//		} else if (entry.getIncomeAssetId() != null) {
//			entryGroupType = EntryGroupType.INCOME;
//		} else if (entry.getExpenseAssetId() != null) {
//			entryGroupType = EntryGroupType.EXPENSE;
//		} else {
//			throw new BlueskyException(BookkeepingErrorCode.NOT_EXIST_ENTRYGROUPTYPE);
//		}
//		entry.setEntryGroupType(entryGroupType);
//		
//		// incomeAsset, expenseAsset이 해당 유저의 정보인지 확인
//		if (entryGroupType == EntryGroupType.INCOME || entryGroupType == EntryGroupType.TRANSFER) {
//			assetService.findById(entry.getIncomeAssetId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_INCOME_ASSET));
//		}
//		if (entryGroupType == EntryGroupType.INCOME || entryGroupType == EntryGroupType.TRANSFER) {
//			assetService.findById(entry.getExpenseAssetId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_EXPENSE_ASSET));
//		}
//		return entryRepository.save(entry);
//	}
//
//	@Override
//	public List<Entry> search(EntryRequestParam entryRequestParam) {
//		Bookkeeping targetBookkeeping = bookkeepingService.findById(entryRequestParam.getBookkeepingId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
//		return entryRepository.findByBookkeepingIdAndEntryDateBetween(targetBookkeeping.getId(), entryRequestParam.getStartDate(), entryRequestParam.getEndDate());
//	}
//	
//	@Override
//	public List<Entry> findByBookkeepingIdAndEntryDateBetween(UUID bookkeepingId, ZonedDateTime startDate, ZonedDateTime endDate) {
//		return entryRepository.findByBookkeepingIdAndEntryDateBetween(bookkeepingId, startDate, endDate);
//	}
//
//	@Override
//	public Entry update(Entry entry) {
//		bookkeepingService.findById(entry.getBookkeepingId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
//		Entry targetEntry = entryRepository.findById(entry.getId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_ENTRY));
//		
//		targetEntry.setAmount(entry.getAmount());
//		targetEntry.setEntryDate(entry.getEntryDate());
//		targetEntry.setMemo(entry.getMemo());
//		// TOOD 변경 할 속성 처리 - 하위 domain 에 대해 변경은 각 domain의 validation 체크가 필요함 
//		
//		return entryRepository.save(targetEntry);
//	}
//
//	@Override
//	public void delete(Entry entry) {
//		bookkeepingService.findById(entry.getBookkeepingId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
//		Entry targetEntry = entryRepository.findById(entry.getId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_ENTRY));
//		entryRepository.delete(targetEntry);
//	}
//
//	
//	@Override
//	public void deleteByBookkeepingId(UUID bookkeepingId) {
//		entryRepository.deleteByBookkeepingId(bookkeepingId);
//	}
//	
//}
