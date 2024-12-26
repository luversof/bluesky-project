package net.luversof.api.bookkeeping;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.github.f4b6a3.uuid.alt.GUID;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.api.bookkeeping.constant.TestConstant;
import net.luversof.api.bookkeeping.domain.Entry;
import net.luversof.api.bookkeeping.repository.mariadb.AssetRepository;
import net.luversof.api.bookkeeping.repository.mariadb.BookkeepingRepository;
import net.luversof.api.bookkeeping.repository.mariadb.EntryRepository;
import net.luversof.api.bookkeeping.repository.mariadb.EntryTypeRepository;

@Slf4j
//@Rollback(false)
class EntryTest implements GeneralTest {
	
	@Autowired
	private BookkeepingRepository bookkeepingRepository;
	
	@Autowired
	private AssetRepository assetRepository;
	
	@Autowired
	private EntryTypeRepository entryTypeRepository;
	
	@Autowired
	private EntryRepository entryRepository;
	
	private UUID userId = TestConstant.USER_ID;
	
	@Test
	void saveTest() {
		var bookkeeping = bookkeepingRepository.findByUserId(userId).get(0);
		var account = assetRepository.findByBookkeepingId(bookkeeping.getId()).get(0);
		var entryTransactionType = entryTypeRepository.findByBookkeepingId(bookkeeping.getId()).get(0);
		
		var transactionRecord = new Entry();
		
//		
//		transactionRecord.setAssetId(account.getId());
//		transactionRecord.setTransactionGroupId(GUID.v7().toUUID());
//		transactionRecord.setCredit(new BigDecimal("123123"));
////		entry.setEntryDate(ZonedDateTime.now());
//		transactionRecord.setMemo("메모");
		
		entryRepository.save(transactionRecord);
	}
	
	
	@Test
	@DisplayName("bookkeepingId 기준 entry 전체 삭제")
	void deleteByBookkeepingId() {
		var bookkeeping = bookkeepingRepository.findByUserId(userId).get(0);
		
		var bookkeepingId = bookkeeping.getId();
		
		long result = entryRepository.deleteByBookkeepingId(bookkeepingId);
		
		log.debug("result : {}", result);
	}
	
}
