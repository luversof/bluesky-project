package net.luversof.api.stock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import net.luversof.api.stock.domain.Account;
import net.luversof.api.stock.domain.StockItem;

/**
 * 계좌·종목 생성의 필수값 검증을 고정한다.
 *
 * <p>예전에는 검사가 없어서, 이름 없는 계좌 생성 요청이 {@code appendJsonConfig} 의 {@code
 * account.getName().contains(...)} 에서 터졌다(실측: {@code POST /api/account} 에 {@code userId} 만 보내면
 * <b>500 NullPointerException</b>). api-stock 은 인증 없이 노출돼 있어 잘못된 요청이 그대로 서버 오류 알림이 된다.
 *
 * <p>종목은 심볼이 비면 시세 조회와 지급이력 등록이 그 종목을 찾지 못하는데, 저장은 성공해서 화면에만 남는다.
 *
 * <p>저장소가 없어도 검증 단계에서 예외가 나므로 서비스를 그대로 만들어 부른다.
 */
class CreateValidationTest {

  private final AccountService accountService = new AccountService();
  private final StockItemService stockItemService = new StockItemService();

  private Account account(String name, UUID userId) {
    var account = new Account();
    account.setName(name);
    account.setUserId(userId);
    return account;
  }

  private StockItem stockItem(String symbol, String name) {
    var stockItem = new StockItem();
    stockItem.setSymbol(symbol);
    stockItem.setName(name);
    return stockItem;
  }

  @Test
  void 이름_없는_계좌는_400_이다() {
    var thrown =
        assertThrows(
            ResponseStatusException.class,
            () -> accountService.createAccount(account(null, UUID.randomUUID())));
    assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatusCode());
    assertTrue(thrown.getMessage().contains("name"));

    assertThrows(
        ResponseStatusException.class,
        () -> accountService.createAccount(account("   ", UUID.randomUUID())));
  }

  @Test
  void 소유자_없는_계좌는_400_이다() {
    var thrown =
        assertThrows(
            ResponseStatusException.class,
            () -> accountService.createAccount(account("동양증권", null)));
    assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatusCode());
    assertTrue(thrown.getMessage().contains("userId"));
  }

  @Test
  void 계좌_자체가_없으면_400_이다() {
    assertThrows(ResponseStatusException.class, () -> accountService.createAccount(null));
  }

  @Test
  void 심볼이나_이름_없는_종목은_400_이다() {
    var noSymbol =
        assertThrows(
            ResponseStatusException.class,
            () -> stockItemService.createStockItem(stockItem(null, "삼성전자")));
    assertEquals(HttpStatus.BAD_REQUEST, noSymbol.getStatusCode());
    assertTrue(noSymbol.getMessage().contains("symbol"));

    var noName =
        assertThrows(
            ResponseStatusException.class,
            () -> stockItemService.createStockItem(stockItem("005930", "  ")));
    assertTrue(noName.getMessage().contains("name"));

    assertThrows(ResponseStatusException.class, () -> stockItemService.createStockItem(null));
  }
}
