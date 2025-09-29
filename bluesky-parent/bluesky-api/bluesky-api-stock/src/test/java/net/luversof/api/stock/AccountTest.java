package net.luversof.api.stock;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.api.stock.repository.AccountRepository;

@Slf4j
class AccountTest implements GeneralTest {
	
	@Autowired
	AccountRepository accountRepository;

	@Test
	void test() {
		var list = accountRepository.findAll();
		log.debug("list : {}", list);
	}
}
