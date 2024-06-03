package net.luversof.api.bookkeeping.base;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.github.f4b6a3.uuid.alt.GUID;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.api.bookkeeping.base.domain.TransactionRecord;
import net.luversof.api.bookkeeping.base.repository.mariadb.AssetRepository;
import net.luversof.api.bookkeeping.base.repository.mariadb.LedgerRepository;
import net.luversof.api.bookkeeping.base.repository.mariadb.TransactionRecordRepository;
import net.luversof.api.bookkeeping.base.repository.mariadb.TransactionTypeRepository;
import net.luversof.api.bookkeeping.constant.TestConstant;

@Slf4j
//@Rollback(false)
class EntryTest implements GeneralTest {
	
	@Autowired
	private LedgerRepository bookkeepingRepository;
	
	@Autowired
	private AssetRepository accountRepository;
	
	@Autowired
	private TransactionTypeRepository entryTransactionTypeRepository;
	
	@Autowired
	private TransactionRecordRepository entryRepository;
	
	@Test
	void saveTest() {
		var bookkeeping = bookkeepingRepository.findByUserId(TestConstant.USER_ID).get(0);
		var account = accountRepository.findByLedgerId(bookkeeping.getId()).get(0);
		var entryTransactionType = entryTransactionTypeRepository.findByLedgerId(bookkeeping.getId()).get(0);
		
		var transactionRecord = new TransactionRecord();
		
		
		transactionRecord.setAssetId(account.getId());
		transactionRecord.setTransactionGroupId(GUID.v7().toUUID());
		transactionRecord.setCredit(new BigDecimal("123123"));
//		entry.setEntryDate(ZonedDateTime.now());
		transactionRecord.setMemo("메모");
		
		entryRepository.save(transactionRecord);
	}
	
}
