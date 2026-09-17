package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.jdbc.repository.query.Query;

import net.luversof.api.stock.domain.StockItemCashFlow;
import net.luversof.api.stock.repository.TradeRepository;
import net.luversof.api.stock.web.dto.response.StockCashFlowResponse;

/**
 * 종목별 순현금흐름 &mdash; 자산 현황 · 종목 상세의 연평균 수익률(XIRR) 재료.
 *
 * <p>2026-09-17 사용자 결정으로 연평균을 복리 환산(최초 매수일부터 보유 원가 전액)에서 XIRR(돈이 오간 날짜)로 바꿨다. 흐름의 부호 · 수수료 · 세금 ·
 * 날짜가 틀리면 화면의 연평균이 조용히 틀린다.
 */
class StockHoldingCashFlowTest {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private static final UUID SAMSUNG = UUID.fromString("00000000-0000-0000-0000-000000000001");

  private static final UUID RISE = UUID.fromString("00000000-0000-0000-0000-000000000002");

  private static StockItemCashFlow row(UUID item, String instant, String amount) {
    return new StockItemCashFlow(item, Instant.parse(instant), new BigDecimal(amount));
  }

  @Test
  void 같은_날의_돈은_존_기준으로_더하고_날짜순으로_둔다() {
    var result =
        StockHoldingPeriodService.netByDay(
            List.of(
                row(SAMSUNG, "2026-09-02T00:00:00Z", "-1369525"),
                // KST 로는 같은 9 월 2 일이다(UTC 로 자르면 9 월 1 일이 된다).
                row(SAMSUNG, "2026-09-01T15:30:00Z", "100000"),
                row(SAMSUNG, "2026-08-04T00:00:00Z", "5000"),
                row(RISE, "2026-09-02T00:00:00Z", "-10"),
                row(null, "2026-09-02T00:00:00Z", "1"),
                new StockItemCashFlow(RISE, null, BigDecimal.ONE),
                new StockItemCashFlow(RISE, Instant.parse("2026-09-03T00:00:00Z"), null)),
            KST);

    assertThat(result.get(SAMSUNG))
        .containsExactly(
            new StockCashFlowResponse(LocalDate.parse("2026-08-04"), new BigDecimal("5000")),
            new StockCashFlowResponse(LocalDate.parse("2026-09-02"), new BigDecimal("-1269525")));
    assertThat(result.get(RISE))
        .as("종목 · 날짜 · 금액이 빠진 줄은 버린다")
        .containsExactly(
            new StockCashFlowResponse(LocalDate.parse("2026-09-02"), new BigDecimal("-10")));
    assertThat(result).hasSize(2);
  }

  @Test
  void 필터는_쉼표로_잇고_비면_좁히지_않는다() {
    TradeRepository repository = mock(TradeRepository.class);
    when(repository.findCashFlowsByStockItem(eq(SAMSUNG), isNull(), eq(RISE.toString())))
        .thenReturn(List.of(row(RISE, "2026-09-02T00:00:00Z", "-10")));
    StockHoldingPeriodService service = new StockHoldingPeriodService();
    service.setTradeRepository(repository);

    var result = service.findCashFlowsByStockItem(SAMSUNG, null, List.of(), List.of(RISE));

    verify(repository).findCashFlowsByStockItem(eq(SAMSUNG), isNull(), eq(RISE.toString()));
    assertThat(result.get(RISE)).hasSize(1);
  }

  /**
   * 금액 규칙은 손익 계산과 같아야 한다 - 흐름의 합(+ 오늘 평가액)이 평가손익(Net) + 실현손익(Net) + 배당 합계와 같았다(실측 2026-09-17, 9 종목
   * 0 원).
   */
  @Test
  void 매수는_수수료와_세금을_더해_나가고_매도는_빼고_들어오고_배당은_세후다() throws Exception {
    String sql =
        TradeRepository.class
            .getMethod("findCashFlowsByStockItem", UUID.class, String.class, String.class)
            .getAnnotation(Query.class)
            .value()
            .replaceAll("\\s+", " ");

    assertThat(sql)
        .contains(
            "CASE WHEN t.\"type\" = 'BUY' THEN -(COALESCE(t.\"price\", 0) * t.\"quantity\" + COALESCE(t.\"fee\", 0) + COALESCE(t.\"tax\", 0))")
        .contains(
            "ELSE COALESCE(t.\"price\", 0) * t.\"quantity\" - COALESCE(t.\"fee\", 0) - COALESCE(t.\"tax\", 0) END AS amount")
        .contains("AND t.\"type\" IN ('BUY', 'SELL')")
        .contains(
            "COALESCE(d.\"grossAmount\", 0) - COALESCE(d.\"tax\", 0) - COALESCE(d.\"fee\", 0) AS amount")
        .as("계좌 · 종목 필터는 매매와 배당 양쪽에 걸려야 한다")
        .containsPattern("t\\.\"account_id\" = ANY\\(string_to_array\\(:accountIds")
        .containsPattern("d\\.\"account_id\" = ANY\\(string_to_array\\(:accountIds")
        .containsPattern("t\\.\"stockItem_id\" = ANY\\(string_to_array\\(:stockItemIds")
        .containsPattern("d\\.\"stockItem_id\" = ANY\\(string_to_array\\(:stockItemIds")
        .contains("a.\"user_id\" = :userId AND t.")
        .contains("a.\"user_id\" = :userId AND d.");
  }
}
