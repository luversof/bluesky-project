package net.luversof.web.gate.stock.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * 매매 화면의 "실현손익"은 한 가지 기준이어야 한다.
 *
 * <p>실현손익에는 출처가 다른 두 값이 있다 — 매도 거래에 기록된 값(증권사 기준)과 앱이 평균단가로 다시 계산한 값. 종목별 표를 만드는 {@code
 * ofStockRealized} 가 두 값을 한 자리에 담는 바람에 같은 화면 안에서 헤드라인·거래목록(기록값 합계 225,584,549)과 계좌별·종목별
 * 표(225,330,99x)가 어긋났다(실측). 두 값을 분리해 싣고, 표시는 기록값으로 통일한다.
 */
class TradeProfitStockRealizedTest {

  private static final UUID STOCK_ITEM_ID = UUID.randomUUID();

  private TradeProfit stockRealized(String recorded, String computedNet) {
    return TradeProfit.ofStockRealized(
        STOCK_ITEM_ID,
        "삼성전자",
        5043,
        1598,
        new BigDecimal("466231000"),
        new BigDecimal("242775200"),
        new BigDecimal("1248142500"),
        new BigDecimal("885617421"),
        new BigDecimal(recorded),
        new BigDecimal(computedNet),
        new BigDecimal("466289914"),
        new BigDecimal("242250230"),
        new BigDecimal("58914"),
        new BigDecimal("39748"),
        new BigDecimal("1885967"));
  }

  @Test
  void 기록된_실현손익과_앱이_계산한_실현손익을_각각_보존한다() {
    TradeProfit row = stockRealized("138569333", "138557498");

    assertThat(row.realizedProfit()).isEqualByComparingTo("138569333");
    assertThat(row.realizedProfitNet()).isEqualByComparingTo("138557498");
    // 두 값이 같은 자리에 담기면(예전 동작) 이 단언이 깨진다.
    assertThat(row.realizedProfit()).isNotEqualByComparingTo(row.realizedProfitNet());
  }

  @Test
  void 집계는_두_기준을_섞지_않는다() {
    var sums =
        TradeProfitAggregator.aggregate(
            List.of(stockRealized("138569333", "138557498"), stockRealized("570", "9438")));

    assertThat(sums.realizedProfit()).isEqualByComparingTo("138569903");
    assertThat(sums.realizedProfitNet()).isEqualByComparingTo("138566936");
  }

  @Test
  void 보유_수량과_매도_수량은_그대로_실린다() {
    TradeProfit row = stockRealized("138569333", "138557498");

    assertThat(row.holdingQuantity()).isEqualTo(5043);
    assertThat(row.totalSellQuantity()).isEqualTo(1598);
    assertThat(row.totalSellAmount()).isEqualByComparingTo("242775200");
    assertThat(row.totalSellProceeds()).isEqualByComparingTo("242250230");
  }
}
