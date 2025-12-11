package net.luversof.api.bookkeeping;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.luversof.GeneralTest;
import net.luversof.api.bookkeeping.constant.AssetTypeCode;
import net.luversof.api.bookkeeping.constant.EntryTypeCode;
import net.luversof.api.bookkeeping.constant.TestConstant;
import net.luversof.api.bookkeeping.domain.AssetType;
import net.luversof.api.bookkeeping.domain.Entry;
import net.luversof.api.bookkeeping.repository.AssetRepository;
import net.luversof.api.bookkeeping.repository.AssetTypeRepository;
import net.luversof.api.bookkeeping.repository.EntryRepository;
import net.luversof.api.bookkeeping.repository.EntryTypeRepository;
import net.luversof.api.bookkeeping.service.EntryService;

//@Rollback(false)
class EntryTest implements GeneralTest {

	private static final Logger log = LoggerFactory.getLogger(EntryTest.class);

	@Autowired
	private AssetRepository assetRepository;

	@Autowired
	private AssetTypeRepository assetTypeRepository;

	@Autowired
	private EntryTypeRepository entryTypeRepository;

	@Autowired
	private EntryRepository entryRepository;

	@Autowired
	private EntryService entryService;

	private UUID bookkeepingId = TestConstant.BOOKKEEPING_ID;

	@Test
	@DisplayName("저장 테스트")
	void createEntry() {
		var assetList = assetRepository.findByBookkeepingId(bookkeepingId);
		var entryTypeList = entryTypeRepository.findByBookkeepingId(bookkeepingId);

		var contraAsset = assetList.stream()
				.filter(asset -> getAssetType(asset.getAssetTypeId()).getCode() == AssetTypeCode.CONTRA_ASSET).findAny()
				.get();
		var cashAsset = assetList.stream()
				.filter(asset -> getAssetType(asset.getAssetTypeId()).getCode() == AssetTypeCode.CASH).findAny().get();
		var incomeEntryType = entryTypeList.stream().filter(entryType -> entryType.getCode() == EntryTypeCode.INCOME)
				.findAny().get();
		var outgoingEntryType = entryTypeList.stream()
				.filter(entryType -> entryType.getCode() == EntryTypeCode.OUTGOING).findAny().get();

		// 수입의 경우 테스트
		{
			var entry = new Entry();
			entry.setBookkeepingId(bookkeepingId);
			entry.setOutgoingAssetId(contraAsset.getId());
			entry.setIncomeAssetId(cashAsset.getId());
			entry.setEntryTypeId(incomeEntryType.getId());
			entry.setAmount(BigDecimal.valueOf(1234));
			entry.setEntryDate(Instant.now());

			var result = entryService.createEntry(entry);
			assertThat(result).isNotNull();
		}

		// 지출의 경우 테스트
		{
			var entry = new Entry();
			entry.setBookkeepingId(bookkeepingId);
			entry.setOutgoingAssetId(cashAsset.getId());
			entry.setIncomeAssetId(contraAsset.getId());
			entry.setEntryTypeId(outgoingEntryType.getId());
			entry.setAmount(BigDecimal.valueOf(123));
			entry.setEntryDate(Instant.now());

			var result = entryService.createEntry(entry);
			assertThat(result).isNotNull();
		}

	}

	private AssetType getAssetType(UUID id) {
		return assetTypeRepository.findById(id).orElseThrow();
	}

	@Test
	@DisplayName("수정 테스트")
	void updateEntry() {
		var entryList = entryRepository.findByBookkeepingId(bookkeepingId);
		var entry = entryList.get(0);
		entry.setAmount(entry.getAmount().add(BigDecimal.valueOf(1)));

		var result = entryService.updateEntry(entry);
		assertThat(result).isNotNull();
	}

	@Test
	@DisplayName("bookkeepingId 기준 entry 전체 삭제")
	void deleteByBookkeepingId() {
		// long result = entryBaseService.deleteByBookkeepingId(bookkeepingId);
		//
		// log.debug("result : {}", result);
	}

}
