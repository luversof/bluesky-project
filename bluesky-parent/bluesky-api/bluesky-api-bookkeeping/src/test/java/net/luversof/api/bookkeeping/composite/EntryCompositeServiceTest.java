package net.luversof.api.bookkeeping.composite;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.api.bookkeeping.base.domain.TransactionRecord;
import net.luversof.api.bookkeeping.base.repository.mariadb.AssetRepository;
import net.luversof.api.bookkeeping.base.repository.mariadb.LedgerRepository;
import net.luversof.api.bookkeeping.base.repository.mariadb.TransactionRecordRepository;
import net.luversof.api.bookkeeping.base.repository.mariadb.TransactionTypeRepository;
import net.luversof.api.bookkeeping.composite.service.TransactionRecordCompositeService;
import net.luversof.api.bookkeeping.constant.TestConstant;

@Slf4j
class EntryCompositeServiceTest  implements GeneralTest {
	
	@Autowired
	private LedgerRepository ledgerRepository;
	
	@Autowired
	private AssetRepository assetRepository;
	
	@Autowired
	private TransactionTypeRepository transactionTypeRepository;
	
	@Autowired
	private TransactionRecordRepository transactionRecordRepository;
	
	@Autowired
	private TransactionRecordCompositeService entryCompositeService;

	@Test
	void serviceSaveTest() {
		var bookkeeping = ledgerRepository.findByUserId(TestConstant.USER_ID).get(0);
		var account = assetRepository.findByLedgerId(bookkeeping.getId()).get(0);
		var transactionType = transactionTypeRepository.findByLedgerId(bookkeeping.getId()).get(0);
		
		var entryList = new ArrayList<TransactionRecord>();
		{
			var entry = new TransactionRecord();
			entry.setAssetId(account.getId());
			entry.setCredit(new BigDecimal("123123"));
			entry.setTransactionTypeId(transactionType.getId());
			entry.setTransactionDate(ZonedDateTime.now());
			entryList.add(entry);
		}
		{
			var entry = new TransactionRecord();
			entry.setAssetId(account.getId());
			entry.setTransactionTypeId(transactionType.getId());
			entry.setDebit(new BigDecimal("123123"));
			entry.setTransactionDate(ZonedDateTime.now());
			entryList.add(entry);
		}
		
		
		entryCompositeService.save(entryList);
	}

}
