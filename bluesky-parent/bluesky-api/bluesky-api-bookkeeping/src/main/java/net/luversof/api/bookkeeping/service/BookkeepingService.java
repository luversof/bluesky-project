package net.luversof.api.bookkeeping.service;

import java.util.UUID;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.luversof.api.bookkeeping.constant.AssetInitialData;
import net.luversof.api.bookkeeping.constant.AssetTypeInitialData;
import net.luversof.api.bookkeeping.constant.BookkeepingError;
import net.luversof.api.bookkeeping.constant.EntryTypeInitialData;
import net.luversof.api.bookkeeping.domain.Bookkeeping;
import net.luversof.api.bookkeeping.repository.mariadb.AssetRepository;
import net.luversof.api.bookkeeping.repository.mariadb.AssetTypeRepository;
import net.luversof.api.bookkeeping.repository.mariadb.BookkeepingRepository;
import net.luversof.api.bookkeeping.repository.mariadb.EntryRepository;
import net.luversof.api.bookkeeping.repository.mariadb.EntryTypeRepository;

@Slf4j
@Service
public class BookkeepingService {
	
	@Setter(onMethod_ = @Autowired)
	private BookkeepingRepository bookkeepingRepository;
	
	@Setter(onMethod_ = @Autowired)
	private AssetTypeRepository assetTypeRepository;
	
	@Setter(onMethod_ = @Autowired)
	private AssetRepository assetRepository;
	
	@Setter(onMethod_ = @Autowired)
	private EntryTypeRepository entryTypeRepository;
	
	@Setter(onMethod_ = @Autowired)
	private EntryRepository entryRepository;
	
	
	/**
	 * 가계부 초기 데이터 생성
	 */
	@Transactional
	public Bookkeeping createBookkeeping(Bookkeeping bookkeeping) {
		
		
		if (!bookkeepingRepository.findByUserId(bookkeeping.getUserId()).isEmpty()) {
			BookkeepingError.ALREADY_EXIST_BOOKKEEPING.throwException();
		}
		
		var bookkeepingResult = bookkeepingRepository.save(bookkeeping);
		
		// 자산 유형 
		var assetTypeList = StreamSupport.stream(assetTypeRepository.saveAll(AssetTypeInitialData.getInitialData(bookkeepingResult.getId())).spliterator(), false).toList();
		
		assetRepository.saveAll(AssetInitialData.getInitialData(bookkeepingResult, assetTypeList));
		
		entryTypeRepository.saveAll(EntryTypeInitialData.getInitialData(bookkeepingResult.getId()));
		
		return bookkeepingResult;
	}
	
	/**
	 * 유저의 가계부 데이터 일괄 삭제
	 * @param userId
	 */
	@Transactional
	public void deleteBookkeepingByUserId(UUID userId) {
		var target = this;
		bookkeepingRepository.findByUserId(userId).forEach(bookkeeping -> target.deleteBookkeepingByBookkeepingId(bookkeeping.getId()));
	}
	
	/**
	 * bookkeepingId 기준 가계부 데이터 일괄 삭제
	 * @param bookkeepingId
	 */
	@Transactional
	public void deleteBookkeepingByBookkeepingId(UUID bookkeepingId) {
		
		if (bookkeepingRepository.findById(bookkeepingId).isEmpty()) {
			BookkeepingError.NOT_EXIST_BOOKKEEPING.throwException();
		}
		
		// delete entry
		var entryCount = entryRepository.deleteByBookkeepingId(bookkeepingId);
		
		// delete entryType
		var entryTypeCount = entryTypeRepository.deleteByBookkeepingId(bookkeepingId);
		
		// delete asset
		var assetCount = assetRepository.deleteByBookkeepingId(bookkeepingId);
		
		// delete assetType
		var assetTypeCount = assetTypeRepository.deleteByBookkeepingId(bookkeepingId);
		
		bookkeepingRepository.deleteById(bookkeepingId);
		
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
