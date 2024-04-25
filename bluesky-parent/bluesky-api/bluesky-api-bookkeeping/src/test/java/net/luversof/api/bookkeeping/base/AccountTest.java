package net.luversof.api.bookkeeping.base;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.api.bookkeeping.base.domain.Account;
import net.luversof.api.bookkeeping.base.repository.mariadb.AccountRepository;
import net.luversof.api.bookkeeping.base.repository.mariadb.AccountTypeRepository;
import net.luversof.api.bookkeeping.base.repository.mariadb.BookkeepingRepository;
import net.luversof.api.bookkeeping.base.service.AccountBaseService;
import net.luversof.api.bookkeeping.constant.TestConstant;

@Slf4j
class AccountTest implements GeneralTest {
	
	@Autowired
	private BookkeepingRepository bookkeepingRepository;
	
	@Autowired
	private AccountRepository accountRepository;
	
	@Autowired
	private AccountTypeRepository accountTypeRepository;

	@Autowired
	private AccountBaseService accountService;
	
	@Test
	void findAll() {
		List<Account> accountList = accountRepository.findAll();
		log.debug("accountList : {}", accountList);
	}
	
	@Test
	void findById() {
		var accountOptional = accountService.findById(UUID.randomUUID());
		log.debug("accountOptional : {}", accountOptional);
	}
	
	
	@Test
	void save() {
		
		var bookkeeping = bookkeepingRepository.findByUserId(TestConstant.USER_ID).get(0);
		
		var accountType = accountTypeRepository.findByBookkeepingId(bookkeeping.getId()).get(0);
		
		
		Account account = new Account();
		account.setName("테스트account");
		account.setBookkeepingId(bookkeeping.getId());
		account.setAccountTypeId(accountType.getId());
		var result = accountRepository.save(account);
		log.debug("result : {}", result);
	}
}
