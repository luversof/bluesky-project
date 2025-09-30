package net.luversof.api.stock;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.api.stock.constant.TestConstant;
import net.luversof.api.stock.domain.Account;
import net.luversof.api.stock.service.AccountService;

@Slf4j
class AccountTest implements GeneralTest {
	
	@Autowired
	AccountService accountService;
	
	UUID userId = TestConstant.USER_ID;

	@Test
	void createAccount() {
		var account = new Account();
		account.setUserId(userId);
		account.setName("테스트계좌");
		
		var result = accountService.createAccount(account);
		log.debug("result : {}", result);
	}
	
	@Test
	void deleteAllByUserId() {
		accountService.deleteAllByUserId(userId);
		log.debug("삭제 완료 : {}", userId);
	}
}
