package net.luversof.api.bookkeeping.base;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.api.bookkeeping.base.domain.Entry;
import net.luversof.api.bookkeeping.base.repository.mariadb.AccountRepository;
import net.luversof.api.bookkeeping.base.repository.mariadb.BookkeepingRepository;
import net.luversof.api.bookkeeping.base.repository.mariadb.EntryRepository;
import net.luversof.api.bookkeeping.base.repository.mariadb.EntryTransactionRepository;
import net.luversof.api.bookkeeping.base.repository.mariadb.EntryTypeRepository;
import net.luversof.api.bookkeeping.constant.TestConstant;

@Slf4j
class EntryTest implements GeneralTest {
	
	@Autowired
	private BookkeepingRepository bookkeepingRepository;
	
	@Autowired
	private AccountRepository accountRepository;
	
	@Autowired
	private EntryTypeRepository entryTypeRepository;
	
	@Autowired
	private EntryTransactionRepository entryTransactionRepository;

	@Autowired
	private EntryRepository entryRepository;
	
	@Test
	void saveTest() {
		var bookkeeping = bookkeepingRepository.findByUserId(TestConstant.USER_ID).get(0);
		var account = accountRepository.findByBookkeepingId(bookkeeping.getId()).get(0);
		var entryType = entryTypeRepository.findByBookkeepingId(bookkeeping.getId()).get(0);
		var entryTransaction = entryTransactionRepository.findByBookkeepingId(bookkeeping.getId()).get(0);
		
		var entry = new Entry();
		entry.setAccountId(account.getId());
		entry.setEntryTypeId(entryType.getId());
		entry.setEntryTransactionId(entryTransaction.getId());
		entry.setCredit(new BigDecimal("123123"));
		entry.setEntryDate(ZonedDateTime.now());
		entry.setMemo("메모");
		
		entryRepository.save(entry);
	}
}
