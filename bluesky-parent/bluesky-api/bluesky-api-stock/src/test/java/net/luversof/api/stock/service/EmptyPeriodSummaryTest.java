package net.luversof.api.stock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.luversof.api.stock.web.dto.response.TradeProfitTimeSeriesPoint;

/**
 * 측정할 게 없는 구간에서 수익률·낙폭을 0% 라고 답하지 않는지 고정한다.
 *
 * <p>실측: 거래가 하나도 없는 구간(2000-01-01~2005-12-31)을 물으면 {@code growthRatePct} 와 {@code maxDrawdownPct} 는
 * {@code null} 인데 {@code timeWeightedReturnPct} 와 {@code currentDrawdownPct} 만 {@code 0.0} 이 나갔다.
 * TWR 지수와 고점 지수가 둘 다 1.0 으로 초기화된 채 단 하루도 곱해지지 않아 {@code (1.0-1.0)*100} 이 그대로 나간 것이다.
 *
 * <p>화면은 이 두 값에 이미 null 분기를 갖고 있어서(-- 로 표시), 0.0 이 들어오면 오히려 "시간가중수익률 +0.00%" 로 그리고, 게다가 {@code
 * currentDrawdownPct > -0.01} 판정에 걸려 <b>"신고점"</b> 이라고 표시했다. 보유가 아예 없던 구간에 대한 답으로 틀렸다.
 */
class EmptyPeriodSummaryTest {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private TradeProfitTimeSeriesPoint point(String date, String value, String cost) {
    LocalDate day = LocalDate.parse(date);
    return new TradeProfitTimeSeriesPoint(
        day.atStartOfDay(KST).toInstant(),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        0L,
        0L,
        0L,
        new BigDecimal(value),
        new BigDecimal(cost),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        day);
  }

  @Test
  void 보유가_없던_구간은_수익률과_낙폭이_측정되지_않음이다() {
    var series =
        List.of(
            point("2000-01-03", "0", "0"),
            point("2000-01-04", "0", "0"),
            point("2000-01-05", "0", "0"));

    var summary = TradeProfitService.summarizeSeries(series, KST, false);

    assertNull(summary.timeWeightedReturnPct(), "곱해진 날이 하루도 없으면 TWR 은 0% 가 아니라 측정 불가다");
    assertNull(summary.currentDrawdownPct(), "같은 이유로 현재 낙폭도 측정 불가다 — 0.0 이면 화면이 '신고점'이라 그린다");
    assertNull(summary.growthRatePct());
    assertNull(summary.maxDrawdownPct());
  }

  @Test
  void 보유가_있던_구간은_그대로_수치가_나온다() {
    var series =
        List.of(
            point("2020-01-02", "1000000", "1000000"),
            point("2020-01-03", "1100000", "1000000"),
            point("2020-01-06", "1210000", "1000000"));

    var summary = TradeProfitService.summarizeSeries(series, KST, false);

    assertNotNull(summary.timeWeightedReturnPct(), "실제로 곱해진 날이 있으면 값이 나와야 한다");
    // 10% 씩 두 번 -> 21%
    assertEquals(21.0d, summary.timeWeightedReturnPct(), 0.0001d);
    assertNotNull(summary.currentDrawdownPct());
    // 계속 오르기만 했으므로 현재 낙폭 0(신고점)
    assertEquals(0.0d, summary.currentDrawdownPct(), 0.0001d);
  }

  @Test
  void 시리즈가_비면_전부_비어_있다() {
    var summary = TradeProfitService.summarizeSeries(List.of(), KST, false);
    assertNull(summary.timeWeightedReturnPct());
    assertNull(summary.currentDrawdownPct());
  }
}
