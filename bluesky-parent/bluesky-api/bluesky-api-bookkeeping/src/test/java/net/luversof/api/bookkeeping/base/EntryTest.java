package net.luversof.api.bookkeeping.base;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

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
import net.luversof.api.bookkeeping.constant.TestConstant;

@Slf4j
//@Rollback(false)
class EntryTest implements GeneralTest {
	
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
	
	@Test
	void saveTest() {
		var bookkeeping = bookkeepingRepository.findByUserId(TestConstant.USER_ID).get(0);
		var account = accountRepository.findByBookkeepingId(bookkeeping.getId()).get(0);
		var entryTransactionType = entryTransactionTypeRepository.findByBookkeepingId(bookkeeping.getId()).get(0);
		
		var entryTransaction = new EntryTransaction();
		entryTransaction.setTransactionDate(ZonedDateTime.now());
		entryTransaction.setMemo("메모");
		entryTransactionRepository.save(entryTransaction);
		
		var entry = new Entry();
		entry.setAccountId(account.getId());
		entry.setEntryTransactionId(entryTransaction.getId());
		entry.setCredit(new BigDecimal("123123"));
//		entry.setEntryDate(ZonedDateTime.now());
//		entry.setMemo("메모");
		
		entryRepository.save(entry);
	}
	
}
