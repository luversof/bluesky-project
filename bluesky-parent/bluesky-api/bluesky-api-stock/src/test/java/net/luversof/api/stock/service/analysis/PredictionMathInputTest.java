package net.luversof.api.stock.service.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.luversof.api.stock.domain.MonthlyDividendPayout;
import net.luversof.api.stock.service.analysis.PredictionMath.Bar;
import net.luversof.api.stock.service.analysis.PredictionMath.BaseRates;

/** v3 입력: 종목별 기준 비율 · 분배 기준일 / 배당락일 추정. */
class PredictionMathInputTest {

  @Test
  void 매달_15일_기준일은_다음_달_15일_주말이면_앞_금요일이다() {
    // 2026-11-15 는 일요일 -> 13 일(금). 배당락일은 그 앞 영업일 12 일(목).
    LocalDate next =
        PredictionMath.estimateNextRecordDate(
            LocalDate.of(2026, 10, 15), LocalDate.of(2026, 11, 11));
    assertThat(next).isEqualTo(LocalDate.of(2026, 11, 13));
    assertThat(PredictionMath.exDateOf(next)).isEqualTo(LocalDate.of(2026, 11, 12));
  }

  @Test
  void 월말_기준일은_다음_달들의_마지막_영업일이고_지난_달은_건너뛴다() {
    // 마지막 이력이 9 월 말인데 이미 11-10 이면 10 월 말은 지났다 -> 11 월 마지막 영업일 11-30(월).
    LocalDate next =
        PredictionMath.estimateNextRecordDate(
            LocalDate.of(2026, 9, 30), LocalDate.of(2026, 11, 11));
    assertThat(next).isEqualTo(LocalDate.of(2026, 11, 30));
    // 월요일 기준일의 배당락일은 앞 금요일.
    assertThat(PredictionMath.exDateOf(next)).isEqualTo(LocalDate.of(2026, 11, 27));
  }

  @Test
  void 기준_비율은_채점과_같은_폭으로_나눈다() {
    List<Bar> bars = new ArrayList<>();
    String[] closes = {"100", "101", "101.2", "100", "100", "102"};
    for (int i = 0; i < closes.length; i++) {
      bars.add(new Bar(LocalDate.of(2026, 11, 1).plusDays(i), new BigDecimal(closes[i]), 0));
    }
    BaseRates rates = PredictionMath.baseRates(bars, 60);
    // 1 일 변화 5 개: +1(UP) +0.2(FLAT) -1.19(DOWN) 0(FLAT) +2(UP)
    assertThat(rates.d1UpPct()).isEqualByComparingTo("40");
    assertThat(rates.d1DownPct()).isEqualByComparingTo("20");
    assertThat(rates.d1FlatPct()).isEqualByComparingTo("40");
    // 5 일 변화 1 개: 100 -> 102 (+2%, 폭 1.5% 밖) = UP
    assertThat(rates.d5UpPct()).isEqualByComparingTo("100");
  }

  @Test
  void 분배_정보는_마지막_기준일과_예상_하락폭을_싣는다() {
    MonthlyDividendPayout p = new MonthlyDividendPayout();
    p.setRecordDate(LocalDate.of(2026, 10, 15));
    p.setDividendAmountPerShare(new BigDecimal("50"));
    var info =
        StockAnalysisService.dividendInfo(
            List.of(p), new Bar(LocalDate.of(2026, 11, 10), new BigDecimal("10000"), 0));
    assertThat(info.lastYieldPct()).isEqualByComparingTo("0.5");
    assertThat(info.estimatedExDate()).isEqualTo(LocalDate.of(2026, 11, 12));
    assertThat(StockAnalysisService.dividendInfo(List.of(), null)).isNull();
  }
}
