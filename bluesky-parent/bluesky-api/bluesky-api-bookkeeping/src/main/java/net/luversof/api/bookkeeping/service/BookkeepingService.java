package net.luversof.api.bookkeeping.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.api.bookkeeping.constant.AssetInitialData;
import net.luversof.api.bookkeeping.constant.AssetTypeInitialData;
import net.luversof.api.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.api.bookkeeping.constant.EntryTypeInitialData;
import net.luversof.api.bookkeeping.domain.Bookkeeping;
import net.luversof.api.bookkeeping.repository.AssetRepository;
import net.luversof.api.bookkeeping.repository.AssetTypeRepository;
import net.luversof.api.bookkeeping.repository.BookkeepingRepository;
import net.luversof.api.bookkeeping.repository.EntryRepository;
import net.luversof.api.bookkeeping.repository.EntryTypeRepository;

@Service
public class BookkeepingService {

	private static final Logger log = LoggerFactory.getLogger(BookkeepingService.class);

	@Autowired
	private BookkeepingRepository bookkeepingRepository;

	@Autowired
	private AssetTypeRepository assetTypeRepository;

	@Autowired
	private AssetRepository assetRepository;

	@Autowired
	private EntryTypeRepository entryTypeRepository;

	@Autowired
	private EntryRepository entryRepository;

	public void setBookkeepingRepository(BookkeepingRepository bookkeepingRepository) {
		this.bookkeepingRepository = bookkeepingRepository;
	}

	public void setAssetTypeRepository(AssetTypeRepository assetTypeRepository) {
		this.assetTypeRepository = assetTypeRepository;
	}

	public void setAssetRepository(AssetRepository assetRepository) {
		this.assetRepository = assetRepository;
	}

	public void setEntryTypeRepository(EntryTypeRepository entryTypeRepository) {
		this.entryTypeRepository = entryTypeRepository;
	}

	public void setEntryRepository(EntryRepository entryRepository) {
		this.entryRepository = entryRepository;
	}

	/**
	 * 가계부 초기 데이터 생성
	 */
	@Transactional
	public Bookkeeping createBookkeeping(Bookkeeping bookkeeping) {

		if (!bookkeepingRepository.findByUserId(bookkeeping.getUserId()).isEmpty()) {
			BookkeepingErrorCode.ALREADY_EXIST_BOOKKEEPING.throwException();
		}

		var bookkeepingResult = bookkeepingRepository.save(bookkeeping);

		// 자산 유형
		var assetTypeList = StreamSupport.stream(assetTypeRepository
				.saveAll(AssetTypeInitialData.getInitialData(bookkeepingResult.getId())).spliterator(), false).toList();

		assetRepository.saveAll(AssetInitialData.getInitialData(bookkeepingResult, assetTypeList));

		entryTypeRepository.saveAll(EntryTypeInitialData.getInitialData(bookkeepingResult.getId()));

		return bookkeepingResult;
	}

	public Optional<Bookkeeping> findById(UUID id) {
		return bookkeepingRepository.findById(id);
	}

	public List<Bookkeeping> findByUserId(UUID userId) {
		return bookkeepingRepository.findByUserId(userId);
	}

	/**
	 * 유저의 가계부 데이터 일괄 삭제
	 * 
	 * @param userId
	 */
	@Transactional
	public void deleteAllByUserId(UUID userId) {
		var target = this;
		findByUserId(userId).forEach(bookkeeping -> target.deleteAllByBookkeepingId(bookkeeping.getId()));
	}

	/**
	 * bookkeepingId 기준 가계부 데이터 일괄 삭제
	 * 
	 * @param bookkeepingId
	 */
	@Transactional
	public void deleteAllByBookkeepingId(UUID bookkeepingId) {

		if (bookkeepingRepository.findById(bookkeepingId).isEmpty()) {
			BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING.throwException();
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
