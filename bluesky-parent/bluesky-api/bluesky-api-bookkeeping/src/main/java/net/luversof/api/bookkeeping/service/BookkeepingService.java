package net.luversof.api.bookkeeping.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.luversof.api.bookkeeping.constant.AssetInitialData;
import net.luversof.api.bookkeeping.constant.AssetTypeInitialData;
import net.luversof.api.bookkeeping.constant.EntryTypeInitialData;
import net.luversof.api.bookkeeping.constant.ErrorCode;
import net.luversof.api.bookkeeping.domain.Bookkeeping;
import net.luversof.api.bookkeeping.service.base.AssetBaseService;
import net.luversof.api.bookkeeping.service.base.AssetTypeBaseService;
import net.luversof.api.bookkeeping.service.base.BookkeepingBaseService;
import net.luversof.api.bookkeeping.service.base.EntryBaseService;
import net.luversof.api.bookkeeping.service.base.EntryTypeBaseService;

@Slf4j
@Service
public class BookkeepingService {
	
	@Setter(onMethod_ = @Autowired)
	private BookkeepingBaseService bookkeepingBaseService;
	
	@Setter(onMethod_ = @Autowired)
	private AssetTypeBaseService assetTypeBaseService;
	
	@Setter(onMethod_ = @Autowired)
	private AssetBaseService assetBaseService;
	
	@Setter(onMethod_ = @Autowired)
	private EntryTypeBaseService entryTypeBaseService;
	
	@Setter(onMethod_ = @Autowired)
	private EntryBaseService entryBaseService;
	
	
	/**
	 * 가계부 초기 데이터 생성
	 */
	@Transactional
	public Bookkeeping createBookkeeping(Bookkeeping bookkeeping) {
		
		
		if (!bookkeepingBaseService.findByUserId(bookkeeping.getUserId()).isEmpty()) {
			ErrorCode.ALREADY_EXIST_BOOKKEEPING.throwException();
		}
		
		var bookkeepingResult = bookkeepingBaseService.save(bookkeeping);
		
		var assetTypeList = AssetTypeInitialData.getInitialData(bookkeepingResult.getId());
		assetTypeBaseService.saveAll(assetTypeList);
		
		var assetList = AssetInitialData.getInitialData(bookkeepingResult, assetTypeList);
		assetBaseService.saveAll(assetList);
		
		var entryTypeList = EntryTypeInitialData.getInitialData(bookkeepingResult.getId());
		entryTypeBaseService.saveAll(entryTypeList);
		
		return bookkeepingResult;
	}
	
	/**
	 * 유저의 가계부 데이터 일괄 삭제
	 * @param userId
	 */
	@Transactional
	public void deleteBookkeepingByUserId(UUID userId) {
		var target = this;
		bookkeepingBaseService.findByUserId(userId).forEach(bookkeeping -> target.deleteBookkeepingByBookkeepingId(bookkeeping.getId()));
	}
	
	/**
	 * bookkeepingId 기준 가계부 데이터 일괄 삭제
	 * @param bookkeepingId
	 */
	@Transactional
	public void deleteBookkeepingByBookkeepingId(UUID bookkeepingId) {
		
		if (bookkeepingBaseService.findById(bookkeepingId).isEmpty()) {
			ErrorCode.NOT_EXIST_BOOKKEEPING.throwException();
		}
		
		// delete entry
		var entryCount = entryBaseService.getRepository().deleteByBookkeepingId(bookkeepingId);
		
		// delete entryType
		var entryTypeCount = entryTypeBaseService.getRepository().deleteByBookkeepingId(bookkeepingId);
		
		// delete asset
		var assetCount = assetBaseService.getRepository().deleteByBookkeepingId(bookkeepingId);
		
		// delete assetType
		var assetTypeCount = assetTypeBaseService.getRepository().deleteByBookkeepingId(bookkeepingId);
		
		bookkeepingBaseService.deleteById(bookkeepingId);
		
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
