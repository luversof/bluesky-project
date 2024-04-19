package net.luversof.api.bookkeeping.base;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.api.bookkeeping.base.domain.Account;
import net.luversof.api.bookkeeping.base.repository.AccountRepository;
import net.luversof.api.bookkeeping.base.service.AccountBaseService;

@Slf4j
class AccountTest implements GeneralTest {
	
	@Autowired
	private AccountRepository accountRepository;

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
		Account account = new Account();
		account.setName("테스트account");
		var result = accountRepository.save(account);
		log.debug("result : {}", result);
	}
}
