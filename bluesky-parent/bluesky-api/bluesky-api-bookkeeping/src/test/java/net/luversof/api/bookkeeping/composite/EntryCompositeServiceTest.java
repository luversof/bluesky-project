package net.luversof.api.bookkeeping.composite;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.api.bookkeeping.base.domain.Entry;
import net.luversof.api.bookkeeping.base.domain.EntryTransaction;
import net.luversof.api.bookkeeping.base.repository.mariadb.AccountRepository;
import net.luversof.api.bookkeeping.base.repository.mariadb.BookkeepingRepository;
import net.luversof.api.bookkeeping.base.repository.mariadb.EntryRepository;
import net.luversof.api.bookkeeping.base.repository.mariadb.EntryTransactionRepository;
import net.luversof.api.bookkeeping.base.repository.mariadb.EntryTransactionTypeRepository;
import net.luversof.api.bookkeeping.composite.service.EntryCompositeService;
import net.luversof.api.bookkeeping.constant.TestConstant;

@Slf4j
class EntryCompositeServiceTest  implements GeneralTest {
	
	@Autowired
	private BookkeepingRepository bookkeepingRepository;
	
	@Autowired
	private AccountRepository accountRepository;
	
	@Autowired
	private EntryTransactionTypeRepository entryTransactionTypeRepository;
	
	@Autowired
	private EntryTransactionRepository entryTransactionRepository;

	@Autowired
	private EntryRepository entryRepository;
	
	@Autowired
	private EntryCompositeService entryCompositeService;

	@Test
	void serviceSaveTest() {
		var bookkeeping = bookkeepingRepository.findByUserId(TestConstant.USER_ID).get(0);
		var account = accountRepository.findByBookkeepingId(bookkeeping.getId()).get(0);
		var entryTransactionType = entryTransactionTypeRepository.findByBookkeepingId(bookkeeping.getId()).get(0);
		
		var entryList = new ArrayList<Entry>();
		{
			var entry = new Entry();
			entry.setAccountId(account.getId());
			entry.setCredit(new BigDecimal("123123"));
			
			entryList.add(entry);
		}
		{
			var entry = new Entry();
			entry.setAccountId(account.getId());
			entry.setDebit(new BigDecimal("123123"));
			entryList.add(entry);
		}
		
		var  entryTransaction = new EntryTransaction();
		entryTransaction.setMemo("메모");
		entryTransaction.setTransactionDate(ZonedDateTime.now());
		entryTransaction.setEntryTransactionTypeId(entryTransactionType.getId());
		
		entryCompositeService.save(entryTransaction, entryList);
	}

}
