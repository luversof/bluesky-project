package net.luversof.api.stock;

import java.util.UUID;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import net.luversof.GeneralTest;
import net.luversof.api.stock.constant.TestConstant;
import net.luversof.api.stock.domain.Account;
import net.luversof.api.stock.service.AccountService;

/**
 * 실사용자 데이터를 실제로 바꾸는 개발용 도구다. 자동 실행에서 돌면 안 된다. 실측 사고(2026-08-22): 프로필을 주고 AccountTest 를 돌리자
 * deleteAllByUserId 가 계좌 7 -> 0, 거래 250 -> 0, 배당 193 -> 0 으로 지웠다. 원장은 시트 재가져오기로 되돌렸지만 계좌
 * 설정(manualPrincipalAmount)은 복구 경로가 없어 잃었다. 필요할 때 이 애노테이션을 손으로 떼고 쓸 것.
 */
@Disabled("실사용자 데이터를 지운다 - 필요할 때만 손으로 실행")
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
