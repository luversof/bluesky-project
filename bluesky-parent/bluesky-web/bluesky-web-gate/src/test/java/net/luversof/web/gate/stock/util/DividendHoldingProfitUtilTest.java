package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.dto.view.DividendYieldGroupView;

/**
 * 배당 랭킹 표의 손익 &mdash; 평가 손익(a) + 실현 손익(b) + 배당금(c) = 합산 손익(d), 합산 수익률 = d / 지금까지 산 총액. 사용자 요청 · 승인
 * 2026-09-28("평가금액이 원금에 대해 수익인지 손해인지", "합산 수익률도", "실현 손익도" → 권장안).
 */
class DividendHoldingProfitUtilTest {

  private static DividendYieldGroupView row(UUID id, String net) {
    BigDecimal value = new BigDecimal(net);
    return new DividendYieldGroupView(
        id, "종목", value, value, value, value, value, null, null, null, null, null, null, 1L, null);
  }

  /**
   * 평가금액 90 만 · 평가 손실 10 만(원금 100 만) · 실현 +2 만 · 배당 15 만 · 산 총액 200 만: 평가 -10%, 합산 +7 만 · +3.5%.
   */
  @Test
  void 평가_더하기_실현_더하기_배당이_합산이고_수익률은_산_총액_대비() {
    var profit =
        DividendHoldingProfitUtil.of(
            new BigDecimal("900000"),
            new BigDecimal("-100000"),
            new BigDecimal("20000"),
            new BigDecimal("150000"),
            new BigDecimal("2000000"));

    assertThat(profit.held()).isTrue();
    assertThat(profit.principal()).isEqualByComparingTo("1000000");
    assertThat(profit.evaluationProfitPct()).isEqualByComparingTo("-10.00");
    assertThat(profit.combinedProfit()).isEqualByComparingTo("70000");
    assertThat(profit.combinedProfitPct())
        .as("분모는 지금 원금 100 만이 아니라 산 총액 200 만")
        .isEqualByComparingTo("3.50");
  }

  /** 다 판 종목: 평가 손익은 없지만 합산은 실현 + 배당이고, 산 총액이 있으니 합산 수익률도 나온다. */
  @Test
  void 다_판_종목도_합산_손익과_수익률이_있다() {
    var sold =
        DividendHoldingProfitUtil.of(
            null,
            null,
            new BigDecimal("43311896"),
            new BigDecimal("102040"),
            new BigDecimal("52800000"));

    assertThat(sold.held()).isFalse();
    assertThat(sold.evaluationProfit()).isNull();
    assertThat(sold.evaluationProfitPct()).isNull();
    assertThat(sold.combinedProfit()).isEqualByComparingTo("43413936");
    assertThat(sold.combinedProfitPct()).isEqualByComparingTo("82.22");
  }

  /** 매매 기록이 없고 배당만 있는 종목(실측: 하나금융지주): 합산은 배당뿐, 산 총액이 없어 수익률은 없다. 판 적 없으면 실현은 null(0 원과 다르다). */
  @Test
  void 매매_없이_배당만_있으면_합산은_배당이고_수익률은_없다() {
    var dividendOnly =
        DividendHoldingProfitUtil.of(null, null, BigDecimal.ZERO, new BigDecimal("2100"), null);

    assertThat(dividendOnly.realizedProfit()).isNull();
    assertThat(dividendOnly.combinedProfit()).isEqualByComparingTo("2100");
    assertThat(dividendOnly.combinedProfitPct()).isNull();
  }

  /** 합계: 평가는 들고 있는 행만, 실현 · 배당 · 산 총액은 모든 행 - 합계 줄도 "평가 + 실현 + 배당 = 합산" 이 보이는 그대로. */
  @Test
  void 합계_줄도_평가_실현_배당의_합이_합산이다() {
    UUID held = UUID.randomUUID();
    UUID sold = UUID.randomUUID();
    var rows = List.of(row(held, "100000"), row(sold, "50000"));
    var values = Map.of(held, new BigDecimal("1100000"));
    var evaluations = Map.of(held, new BigDecimal("100000"));
    var realized = Map.of(held, new BigDecimal("30000"), sold, new BigDecimal("-20000"));
    var buys = Map.of(held, new BigDecimal("1500000"), sold, new BigDecimal("500000"));

    var total = DividendHoldingProfitUtil.total(rows, values, evaluations, realized, buys);

    assertThat(total.evaluationProfit()).isEqualByComparingTo("100000");
    assertThat(total.realizedProfit()).isEqualByComparingTo("10000");
    assertThat(total.dividend()).isEqualByComparingTo("150000");
    assertThat(total.combinedProfit())
        .isEqualByComparingTo(
            total.evaluationProfit().add(total.realizedProfit()).add(total.dividend()));
    // 260,000 / 2,000,000
    assertThat(total.combinedProfitPct()).isEqualByComparingTo("13.00");
    assertThat(DividendHoldingProfitUtil.total(List.of(), values, evaluations, realized, buys))
        .isNull();
  }
}
