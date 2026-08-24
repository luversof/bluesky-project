package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.domain.TradeProfit;

/**
 * 화면에 적는 평가손익이 빼지 않은 매수 수수료를 계산하는 규칙을 고정한다.
 *
 * <p>거래 행이 증권사 기록값이라 화면 손익은 기본 기준({@code evaluationProfit})으로 통일했다. 그 기준은 매수 수수료를 원가로 보지 않으므로
 * ({@code currentTotalCostNet = currentTotalCost + fee}), 표시되는 평가손익에는 보유분 매수 수수료가 빠져 있지 않다. 그 금액을
 * 밝히지 않으면 수수료가 이미 반영된 값으로 오해한다(실측 2026-08-23: 24,986 원).
 */
class StockProfitBasisUtilTest {

  /** 평가손익 두 기준만 채운 최소 행. */
  private TradeProfit row(String evaluationProfit, String evaluationProfitNet) {
    BigDecimal[] fields = new BigDecimal[26];
    Arrays.fill(fields, BigDecimal.ZERO);
    return new TradeProfit(
        null,
        "종목",
        null,
        "계좌",
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        0,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        0,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        evaluationProfit == null ? null : new BigDecimal(evaluationProfit),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        evaluationProfitNet == null ? null : new BigDecimal(evaluationProfitNet),
        BigDecimal.ZERO,
        null);
  }

  @Test
  void 두_기준의_차이가_빠진_수수료다() {
    assertThat(StockProfitBasisUtil.excludedHoldingBuyFee(List.of(row("1000", "900"))))
        .isEqualByComparingTo("100");
  }

  @Test
  void 여러_행이면_합산한다() {
    assertThat(
            StockProfitBasisUtil.excludedHoldingBuyFee(
                List.of(row("1000", "900"), row("-500", "-530"), row("0", "0"))))
        .isEqualByComparingTo("130");
  }

  /** 뺄 수 없는 행(둘 중 하나가 없음)은 0 으로 치지 않고 건너뛴다. */
  @Test
  void 한쪽이_없는_행은_건너뛴다() {
    assertThat(
            StockProfitBasisUtil.excludedHoldingBuyFee(
                List.of(row("1000", null), row(null, "900"), row("1000", "900"))))
        .isEqualByComparingTo("100");
  }

  @Test
  void 행이_없거나_null_이면_0_이다() {
    assertThat(StockProfitBasisUtil.excludedHoldingBuyFee(null)).isEqualByComparingTo("0");
    assertThat(StockProfitBasisUtil.excludedHoldingBuyFee(List.of())).isEqualByComparingTo("0");
  }

  /** 기준이 같아지면(수수료 0) 안내가 뜨지 않아야 한다 - 늘 뜨는 줄은 곧 무시된다. */
  @Test
  void 수수료가_없으면_0_이다() {
    assertThat(StockProfitBasisUtil.excludedHoldingBuyFee(List.of(row("1000", "1000"))))
        .isEqualByComparingTo("0");
  }
}
