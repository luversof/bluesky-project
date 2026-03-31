package net.luversof.api.stock;

import java.util.UUID;
import net.luversof.GeneralTest;
import net.luversof.api.stock.constant.TestConstant;
import net.luversof.api.stock.domain.Account;
import net.luversof.api.stock.service.AccountService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

class AccountTest implements GeneralTest {

    private static final Logger log = LoggerFactory.getLogger(AccountTest.class);

    @Autowired AccountService accountService;

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
