package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import net.luversof.api.stock.constant.TradeType;
import net.luversof.api.stock.domain.Account;
import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.domain.Trade;
import net.luversof.api.stock.web.dto.request.TradeSearchRequest;
import net.luversof.api.stock.web.dto.response.TradeResponse;

/**
 * 거래 목록이 종목 이름을 어떻게 읽는지 고정한다.
 *
 * <p>이 경로는 종목의 '이름'만 쓴다. 예전에는 {@code stockItemService.findAll()} 로 종목 테이블을 통째로 읽었는데, 그 메서드는 태그
 * 테이블까지 한 번 더 읽고 조회량이 사용자와 무관하게 전체 종목 수를 따라간다(실측: 전체 86개 중 이 사용자가 거래한 것은 42개, {@code /api/trade} 한
 * 요청의 DB 커넥션 획득 6회).
 *
 * <p>그래서 거래에 실제로 등장하는 종목만 태그 없이 읽어야 한다.
 */
@ExtendWith(MockitoExtension.class)
class TradeHistoryStockItemLookupTest {

  @Mock private AccountService accountService;
  @Mock private TradeService tradeService;
  @Mock private StockItemService stockItemService;

  @InjectMocks private TradeProfitService tradeProfitService;

  private static Trade trade(UUID accountId, UUID stockItemId) {
    Trade trade = new Trade();
    trade.setId(UUID.randomUUID());
    trade.setAccountId(accountId);
    trade.setStockItemId(stockItemId);
    trade.setType(TradeType.BUY);
    trade.setQuantity(1);
    trade.setPrice(BigDecimal.TEN);
    trade.setTradeDate(Instant.parse("2026-01-02T00:00:00Z"));
    return trade;
  }

  private static StockItem stockItem(UUID id, String name) {
    StockItem item = new StockItem();
    item.setId(id);
    item.setName(name);
    return item;
  }

  @Test
  void 거래에_등장하는_종목만_태그없이_읽는다() {
    UUID userId = UUID.randomUUID();
    UUID accountId = UUID.randomUUID();
    UUID heldA = UUID.randomUUID();
    UUID heldB = UUID.randomUUID();

    Account account = new Account();
    account.setId(accountId);
    account.setUserId(userId);
    when(accountService.findByUserId(userId)).thenReturn(List.of(account));
    when(tradeService.findByAccountIdIn(List.of(accountId)))
        .thenReturn(
            List.of(trade(accountId, heldA), trade(accountId, heldB), trade(accountId, heldA)));
    when(stockItemService.findAllByIdWithoutTags(any()))
        .thenReturn(List.of(stockItem(heldA, "가나다"), stockItem(heldB, "라마바")));

    List<TradeResponse> result =
        tradeProfitService.getTradeHistory(new TradeSearchRequest(userId, null, null, null, null));

    // 이름이 실제로 붙는다.
    assertThat(result).hasSize(3);
    assertThat(result).extracting(TradeResponse::stockItemName).containsOnly("가나다", "라마바");

    // 전체 종목 스캔은 하지 않는다.
    verify(stockItemService, never()).findAll();

    // 조회 대상은 거래에 등장한 종목뿐이다(중복 없이).
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Iterable<UUID>> captor = ArgumentCaptor.forClass(Iterable.class);
    verify(stockItemService).findAllByIdWithoutTags(captor.capture());
    List<UUID> requested = new java.util.ArrayList<>();
    captor.getValue().forEach(requested::add);
    assertThat(requested).hasSize(2).containsExactlyInAnyOrder(heldA, heldB);
    assertThat(Set.copyOf(requested)).doesNotContain(UUID.randomUUID());
  }

  /** 계좌가 없으면 종목 조회 자체를 하지 않는다. */
  @Test
  void 계좌가_없으면_종목을_읽지_않는다() {
    UUID userId = UUID.randomUUID();
    when(accountService.findByUserId(userId)).thenReturn(List.of());

    assertThat(
            tradeProfitService.getTradeHistory(
                new TradeSearchRequest(userId, null, null, null, null)))
        .isEmpty();

    verify(stockItemService, never()).findAll();
    verify(stockItemService, never()).findAllByIdWithoutTags(any());
  }
}
