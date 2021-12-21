package net.luversof.bookkeeping.service;


import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.bookkeeping.constant.EntryGroupType;
import net.luversof.bookkeeping.domain.Asset;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.domain.Entry;
import net.luversof.bookkeeping.domain.EntryGroup;
import net.luversof.bookkeeping.domain.web.EntryRequestParam;
import net.luversof.bookkeeping.repository.EntryRepository;
import net.luversof.bookkeeping.util.BookkeepingUtils;

@Service
public class EntryService {
	
	@Autowired
	private EntryRepository entryRepository;
	
	@Autowired
	private EntryGroupService entryGroupService;
	
	@Autowired
	private AssetService assetService;

	/**
	 * income / expense / transfer
	 * @param entry
	 * @return
	 */
	public Entry createUserEntry(Entry entry) {
		Bookkeeping bookkeeping = BookkeepingUtils.getBookkeepingService().getUserBookkeeping(entry.getBookkeeping().getUserId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		entry.setBookkeeping(bookkeeping);
		
		// 요청이 income인 경우 expenseAsset 삭제 처리 & incomeAsset 검증
		if (entry.getEntryGroupType() == EntryGroupType.INCOME) {
			entry.setExpenseAsset(null);
			
			if (entry.getIncomeAsset() == null) {
				throw new BlueskyException(BookkeepingErrorCode.NOT_EXIST_INCOME_ASSET);
			}
		}
		
		// 요청이 expense인 경우 incomeAsset 삭제 처리 & expenseAset 검증
		if (entry.getEntryGroupType() == EntryGroupType.EXPENSE) {
			entry.setIncomeAsset(null);
			
			if (entry.getExpenseAsset() == null) {
				throw new BlueskyException(BookkeepingErrorCode.NOT_EXIST_EXPENSE_ASSET);
			}
		}
		
		// 요청이 transfer 인 경우 entryGroup 삭제 처리
		if (entry.getEntryGroupType() == EntryGroupType.TRANSFER) {
			entry.setEntryGroup(null);
			
			if (entry.getIncomeAsset() == null) {
				throw new BlueskyException(BookkeepingErrorCode.NOT_EXIST_INCOME_ASSET);
			}
			if (entry.getExpenseAsset() == null) {
				throw new BlueskyException(BookkeepingErrorCode.NOT_EXIST_EXPENSE_ASSET);
			}
		}
		
		// incomeAsset, expenseAsset이 해당 유저의 정보인지 확인
		if (entry.getIncomeAsset() != null) {
			Asset targetAsset = assetService.findById(entry.getIncomeAsset().getId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_INCOME_ASSET));
			if (!targetAsset.getBookkeeping().getUserId().equals(entry.getBookkeeping().getUserId())) {
				throw new BlueskyException(BookkeepingErrorCode.NOT_OWNER_INCOME_ASSET);
			}
			entry.setIncomeAsset(targetAsset);
		}
		
		if (entry.getExpenseAsset() != null) {
			Asset targetAsset = assetService.findById(entry.getExpenseAsset().getId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_EXPENSE_ASSET));
			if (!targetAsset.getBookkeeping().getUserId().equals(entry.getBookkeeping().getUserId())) {
				throw new BlueskyException(BookkeepingErrorCode.NOT_OWNER_EXPENSE_ASSET);
			}
			entry.setExpenseAsset(targetAsset);
		}
		
		if (entry.getEntryGroup() != null) {
			EntryGroup entryGroup = entryGroupService.findById(entry.getEntryGroup().getId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_ENTRYGROUP));
			if (!entryGroup.getBookkeeping().getUserId().equals(entry.getBookkeeping().getUserId())) {
				throw new BlueskyException(BookkeepingErrorCode.NOT_OWNER_ENTRYGROUP);
			}
			entry.setEntryGroup(entryGroup);
		}
		
		if (entry.getMemo() == null || entry.getMemo().isBlank()) {
			if (entry.getEntryGroupType() == EntryGroupType.TRANSFER) {
				entry.setMemo(MessageFormat.format("{0} -> {1}", entry.getExpenseAsset().getName(), entry.getIncomeAsset().getName()));
			} else {
				entry.setMemo(entry.getEntryGroup().getName());
			}
		}
		
		return entryRepository.save(entry);
	}

	/**
	 * 기간 기준 검색
	 * @param entrySearchInfo
	 * @return
	 */
	public List<Entry> searchUserEntry(EntryRequestParam entryRequestParam) {
		Bookkeeping targetBookkeeping = BookkeepingUtils.getBookkeepingService().getUserBookkeeping(entryRequestParam.getBookkeeping().getUserId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		return entryRepository.findByBookkeepingIdAndEntryDateBetween(targetBookkeeping.getId(), entryRequestParam.getStartLocalDate(), entryRequestParam.getEndLocalDate());
	}

	public Entry updateUserEntry(Entry entry) {
		Bookkeeping bookkeeping = BookkeepingUtils.getBookkeepingService().getUserBookkeeping(entry.getBookkeeping().getUserId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
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
		Bookkeeping bookkeeping = BookkeepingUtils.getBookkeepingService().getUserBookkeeping(entry.getBookkeeping().getUserId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
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
	public List<Entry> findByBookkeepingIdAndEntryDateBetween(UUID bookkeepingId, LocalDate startLocalDate, LocalDate endLocalDate) {
		return entryRepository.findByBookkeepingIdAndEntryDateBetween(bookkeepingId, startLocalDate, endLocalDate);
	}
	
	
	public int deleteByBookkeepingId(UUID bookkeepingId) {
		return entryRepository.deleteByBookkeepingIdQuery(bookkeepingId);
	}
	
}
