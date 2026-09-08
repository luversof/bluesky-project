package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.domain.TradeProfit;
import net.luversof.web.gate.stock.util.StockCombinedProfitUtil.Breakdown;
import net.luversof.web.gate.stock.util.StockDividendCoverageUtil.Row;

/**
 * "배당이 평가손실을 얼마나 메웠나".
 *
 * <p>실측 2026-09-08: 보유 ETF 9 종 중 5 종이 배당을 다 더해도 합산 손익이 마이너스였는데, 자산 현황 표의 세 열을 눈으로 빼야만 보였다.
 */
class StockDividendCoverageUtilTest {

  private static final UUID LOSS = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
  private static final UUID COVERED = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
  private static final UUID GAIN = UUID.fromString("00000000-0000-0000-0000-0000000000c3");
  private static final UUID QUIET = UUID.fromString("00000000-0000-0000-0000-0000000000d4");

  private static BigDecimal bd(String v) {
    return new BigDecimal(v);
  }

  private static TradeProfit stock(UUID id, String name, String evaluationProfit) {
    return TradeProfit.ofStockStatus(
        id, name, null, 0, null, bd("1000"), bd(evaluationProfit), null, bd("1000"));
  }

  private static Breakdown breakdown(String dividend, String combined) {
    return new Breakdown(BigDecimal.ZERO, bd(dividend), bd(combined));
  }

  /** 실측 2026-09-08 의 모양: KODEX 리츠 평가 -12,715,200 · 배당 5,385,714 -> 배당이 손실의 42% 를 덮었다. */
  @Test
  void 배당이_손실의_몇_퍼센트를_덮었는지_낸다() {
    List<Row> rows =
        StockDividendCoverageUtil.of(
            List.of(stock(LOSS, "KODEX 리츠", "-12715200")),
            Map.of(LOSS, breakdown("5385714", "-6423117")));

    assertThat(rows).hasSize(1);
    Row row = rows.get(0);
    assertThat(row.coveragePct()).isEqualTo(42);
    assertThat(row.hasLoss()).isTrue();
    assertThat(row.fullyCovered()).isFalse();
    assertThat(row.lossBarPct()).as("손실이 큰 쪽이라 100").isEqualTo(100);
    assertThat(row.dividendBarPct()).isEqualTo(42);
    assertThat(row.combinedProfit()).isEqualByComparingTo("-6423117");
  }

  /**
   * 실측 2026-09-08 의 모양: RISE 200위클리 평가 -234,419 · 배당 9,149,432 -> 손실 전부 상쇄(3,903%). 막대는 배당 쪽이 100.
   */
  @Test
  void 배당이_손실보다_크면_전부_상쇄로_본다() {
    List<Row> rows =
        StockDividendCoverageUtil.of(
            List.of(stock(COVERED, "RISE 200", "-234419")),
            Map.of(COVERED, breakdown("9149432", "8915013")));

    Row row = rows.get(0);
    assertThat(row.fullyCovered()).isTrue();
    assertThat(row.coveragePct()).isEqualTo(3903);
    assertThat(row.dividendBarPct()).isEqualTo(100);
    assertThat(row.lossBarPct()).as("234,419 / 9,149,432 = 3%").isEqualTo(3);
  }

  /** 평가익인 종목은 덮을 손실이 없다 - 비율은 null, 손실 막대는 0. */
  @Test
  void 손실이_없으면_비율이_없다() {
    List<Row> rows =
        StockDividendCoverageUtil.of(
            List.of(stock(GAIN, "삼성전자", "999084921")),
            Map.of(GAIN, breakdown("35340449", "1172994703")));

    Row row = rows.get(0);
    assertThat(row.coveragePct()).isNull();
    assertThat(row.hasLoss()).isFalse();
    assertThat(row.lossBarPct()).isZero();
    assertThat(row.dividendBarPct()).isEqualTo(100);
  }

  /** 손실도 배당도 없는 종목은 카드가 할 말이 없어 빼고, 손실 큰 종목부터 세운다. */
  @Test
  void 손실_큰_종목부터_세우고_할_말_없는_종목은_뺀다() {
    List<Row> rows =
        StockDividendCoverageUtil.of(
            List.of(
                stock(GAIN, "이익", "500"),
                stock(QUIET, "조용", "0"),
                stock(COVERED, "작은손실", "-100"),
                stock(LOSS, "큰손실", "-900")),
            Map.of(
                GAIN, breakdown("10", "510"),
                COVERED, breakdown("50", "-50"),
                LOSS, breakdown("50", "-850")));

    assertThat(rows).extracting(Row::stockItemName).containsExactly("큰손실", "작은손실", "이익");
  }

  /** 종목별 실현·배당 맵에 없는 종목(배당 없음)도 손실이 있으면 나온다 - 배당 0, 상쇄 0%. */
  @Test
  void 배당이_없는_손실_종목은_0_퍼센트로_나온다() {
    List<Row> rows = StockDividendCoverageUtil.of(List.of(stock(LOSS, "무배당", "-1000")), Map.of());

    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).coveragePct()).isZero();
    assertThat(rows.get(0).dividendBarPct()).isZero();
    assertThat(rows.get(0).combinedProfit()).isEqualByComparingTo("-1000");
  }
}
