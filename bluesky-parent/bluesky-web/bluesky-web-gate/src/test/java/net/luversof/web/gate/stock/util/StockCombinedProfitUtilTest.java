package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.domain.TradeProfit;

/**
 * 자산 현황의 종목별 <b>실현손익 · 누적배당 · 합산손익</b>.
 *
 * <p>세 값은 종목 상세에만 있었다. 그래서 "이 종목으로 지금까지 얼마 벌었나" 를 보려면 종목을 하나씩 열어야 했고, 종목끼리 견주는 것은 아예 되지 않았다.
 */
class StockCombinedProfitUtilTest {

  private static final UUID A = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
  private static final UUID B = UUID.fromString("00000000-0000-0000-0000-0000000000b2");

  private static BigDecimal bd(String value) {
    return new BigDecimal(value);
  }

  private static TradeProfit stock(UUID id, String evaluationProfit, String realizedProfit) {
    return TradeProfit.ofStockStatus(
        id,
        "테스트종목",
        bd("1000"),
        10,
        bd("1100"),
        bd("11000"),
        evaluationProfit == null ? null : bd(evaluationProfit),
        realizedProfit == null ? null : bd(realizedProfit),
        bd("10000"));
  }

  /**
   * 합산 = 현재 평가손익 + 누적 실현손익 + 누적 배당.
   *
   * <p>종목 상세의 합산 손익은 <i>기간</i> 평가 변동을 쓰지만, 이 화면은 기간이 없는 '지금 보유' 스냅샷이라 전 기간이 대상이고 전 기간의 평가 변동은 곧 현재
   * 평가손익이다(기초가 0). 그래서 두 화면이 어긋나지 않는다.
   */
  @Test
  void 합산은_평가손익과_실현손익과_배당을_더한_값이다() {
    var byStock =
        StockCombinedProfitUtil.byStockItem(
            List.of(stock(A, "-12444645", "906369")), Map.of(A, bd("5385714")));

    var row = byStock.get(A);
    assertThat(row.realizedProfit()).isEqualByComparingTo(bd("906369"));
    assertThat(row.dividendTotal()).isEqualByComparingTo(bd("5385714"));
    assertThat(row.combinedProfit())
        .as("-12,444,645 + 906,369 + 5,385,714 = -6,152,562 (실측 2026-09-03 값)")
        .isEqualByComparingTo(bd("-6152562"));
  }

  /** 배당이 없는 종목은 0 으로 읽는다. 배당 맵에는 배당을 받은 종목만 들어 있다(실측: 보유 43 종목 중 18). */
  @Test
  void 배당이_없는_종목은_0_으로_읽는다() {
    var byStock = StockCombinedProfitUtil.byStockItem(List.of(stock(A, "100", "50")), Map.of());

    assertThat(byStock.get(A).dividendTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(byStock.get(A).combinedProfit()).isEqualByComparingTo(bd("150"));
  }

  /** 값이 null 로 와도 죽지 않는다. 원격이 평가를 못 낸 종목이 그렇게 온다. */
  @Test
  void null_값을_0_으로_읽는다() {
    var byStock =
        StockCombinedProfitUtil.byStockItem(List.of(stock(A, null, null)), Map.of(A, bd("700")));

    assertThat(byStock.get(A).combinedProfit()).isEqualByComparingTo(bd("700"));
  }

  /** 종목 id 가 없는 줄은 건너뛴다. 키로 쓸 수 없어 화면이 찾지 못한다. */
  @Test
  void 종목_id_가_없으면_건너뛴다() {
    var byStock =
        StockCombinedProfitUtil.byStockItem(
            java.util.Arrays.asList(stock(null, "100", "0"), null, stock(A, "200", "0")), Map.of());

    assertThat(byStock).hasSize(1).containsKey(A);
  }

  /** 합계는 세 값을 각각 더한 것이다. */
  @Test
  void 합계는_세_값을_각각_더한다() {
    var byStock =
        StockCombinedProfitUtil.byStockItem(
            List.of(stock(A, "100", "50"), stock(B, "-30", "20")), Map.of(A, bd("10"), B, bd("5")));

    var total = StockCombinedProfitUtil.total(byStock);
    assertThat(total.realizedProfit()).isEqualByComparingTo(bd("70"));
    assertThat(total.dividendTotal()).isEqualByComparingTo(bd("15"));
    assertThat(total.combinedProfit())
        .as("(100+50+10) + (-30+20+5) = 155")
        .isEqualByComparingTo(bd("155"));
  }

  /** 실현손익은 부호를 살려 더한다. 절댓값을 더하면 손실 난 종목이 이익처럼 얹힌다. */
  @Test
  void 합계의_실현손익은_부호를_살린다() {
    var byStock =
        StockCombinedProfitUtil.byStockItem(
            List.of(stock(A, "0", "500"), stock(B, "0", "-300")), Map.of());

    assertThat(StockCombinedProfitUtil.total(byStock).realizedProfit())
        .isEqualByComparingTo(bd("200"));
  }

  /** 빈 목록과 null 은 0 으로 답한다. */
  @Test
  void 자료가_없으면_0_이다() {
    assertThat(StockCombinedProfitUtil.byStockItem(null, Map.of())).isEmpty();
    assertThat(StockCombinedProfitUtil.byStockItem(List.of(), null)).isEmpty();
    var total = StockCombinedProfitUtil.total(Map.of());
    assertThat(total.combinedProfit()).isEqualByComparingTo(BigDecimal.ZERO);
  }
}
