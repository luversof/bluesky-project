package net.luversof.api.bookkeeping.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.luversof.boot.exception.BlueskyException;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.luversof.api.bookkeeping.constant.AssetInitialData;
import net.luversof.api.bookkeeping.constant.AssetTypeInitialData;
import net.luversof.api.bookkeeping.constant.TransactionTypeInitialData;
import net.luversof.api.bookkeeping.domain.Bookkeeping;

@Slf4j
@Service
public class CompositeService {
	
	@Setter(onMethod_ = @Autowired)
	private BookkeepingService bookkeepingService;
	
	@Setter(onMethod_ = @Autowired)
	private AssetTypeService assetTypeService;
	
	@Setter(onMethod_ = @Autowired)
	private AssetService assetService;
	
	@Setter(onMethod_ = @Autowired)
	private EntryTypeService entryTypeService;
	
	@Setter(onMethod_ = @Autowired)
	private EntryService entryService;
	
	
	/**
	 * 가계부 생성
	 * 1. bookkeeping 생성
	 * 2. 기본적인 데이터 (Bookkeeping, Account, AccountType, EntryTransactionType) 생성
	 */
	@Transactional
	public Bookkeeping initDataSetup(Bookkeeping bookkeeping) {
		
		
		if (!bookkeepingService.findByUserId(bookkeeping.getUserId()).isEmpty()) {
			throw new BlueskyException("bookkeeping.ALREAD_EXIST_BOOKKEEPING");
		}
		
		var bookkeepingResult = bookkeepingService.save(bookkeeping);
		
		var accountTypeList = AssetTypeInitialData.getInitialData(bookkeeping.getId());
		assetTypeService.saveAll(accountTypeList);
		
		var accountList = AssetInitialData.getInitialData(bookkeeping, accountTypeList);
		assetService.saveAll(accountList);
		
		var entryTransactionTypeList = TransactionTypeInitialData.getInitialData(bookkeeping.getId());
		entryTypeService.saveAll(entryTransactionTypeList);
		
		return bookkeepingResult;
	}
	
	/**
	 * 유저의 가계부 데이터 일괄 삭제
	 * @param userId
	 */
	@Transactional
	public void deleteAllDataByUserId(UUID userId) {
		bookkeepingService.findByUserId(userId).forEach(bookkeeping -> deleteAllDataByBookkeepingId(bookkeeping.getId()));
	}
	
	/**
	 * bookkeepingId 기준 가계부 데이터 일괄 삭제
	 * @param bookkeepingId
	 */
	@Transactional
	public void deleteAllDataByBookkeepingId(UUID bookkeepingId) {
		
		if (bookkeepingService.findById(bookkeepingId).isEmpty()) {
			throw new BlueskyException("bookkeeping.NOT_EXIST_BOOKKEEPING");
		}
		
		// delete entry
		var entryCount = entryService.getRepository().deleteByBookkeepingId(bookkeepingId);
		
		// delete entryType
		var entryTypeCount = entryTypeService.getRepository().deleteByBookkeepingId(bookkeepingId);
		
		// delete asset
		var assetCount = assetService.getRepository().deleteByBookkeepingId(bookkeepingId);
		
		// delete assetType
		var assetTypeCount = assetTypeService.getRepository().deleteByBookkeepingId(bookkeepingId);
		
		bookkeepingService.getRepository().deleteById(bookkeepingId);
		
		log.debug("""
				
				==== (s) delete bookkeeping report
				deleteBy bookkeepingId : {}
				entryCount : {}
				entryTypeCount : {}
				assetCount : {}
				assetTypeCount : {}
				==== (e) delete bookkeeping report
				""",
				bookkeepingId,
				entryCount,
				entryTypeCount,
				assetCount,
				assetTypeCount);
		
	}
}
