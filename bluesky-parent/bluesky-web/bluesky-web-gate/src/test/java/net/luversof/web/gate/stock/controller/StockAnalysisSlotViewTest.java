package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.httpexchange.StockAnalysisClient.RecentPrediction;

/** 분석 시점(2026-11-27): 시점별 적중 · 다음 거래일 예측 · 시점 고르기. */
class StockAnalysisSlotViewTest {

  private static RecentPrediction pred(
      String baseDate,
      int horizon,
      String replay,
      String created,
      int seq,
      Boolean hit,
      Boolean late,
      String slot) {
    return new RecentPrediction(
        "005930",
        "삼성전자",
        LocalDate.parse(baseDate),
        horizon,
        "UP",
        60,
        null,
        null,
        "v10",
        replay,
        Instant.parse(created),
        seq,
        null,
        null,
        null,
        hit,
        late,
        slot);
  }

  private static final List<RecentPrediction> PREDS =
      List.of(
          // 11-25: 저녁 실시간(맞음) · 08:30 과거 시점(틀림) · 장중 실시간(맞음, 늦음)
          pred("2026-11-25", 1, null, "2026-11-25T09:00:00Z", 1, true, false, "CLOSE"),
          pred("2026-11-25", 1, "재분석 v10", "2026-11-25T23:30:00Z", 2, false, false, "PREMARKET"),
          pred("2026-11-25", 1, null, "2026-11-26T02:00:00Z", 3, true, true, "INTRADAY"),
          // 11-26: 저녁 실시간 D+1 · D+5(채점 전), 08:30 실시간 D+1(채점 전)
          pred("2026-11-26", 1, null, "2026-11-26T09:05:00Z", 1, null, null, "CLOSE"),
          pred("2026-11-26", 5, null, "2026-11-26T09:05:00Z", 1, null, null, "CLOSE"),
          pred("2026-11-26", 1, null, "2026-11-26T23:31:00Z", 2, null, null, "PREMARKET"));

  @Test
  void 시점별_적중은_시점마다_따로_세고_장중은_늦은_예측도_센다() {
    var hits = StockAnalysisViewController.slotHits(PREDS, "v10", null);
    assertThat(hits)
        .extracting(StockAnalysisViewController.SlotHits::slot)
        .containsExactly("CLOSE", "PREMARKET", "INTRADAY");
    assertThat(hits.get(0).hits().d1Hit()).isEqualTo(1);
    assertThat(hits.get(1).hits().d1Hit()).isZero();
    assertThat(hits.get(2).hits().d1Graded()).isEqualTo(1);
  }

  @Test
  void 다음_거래일_예측은_가장_최근_기준일의_회차를_늦게_만든_것부터() {
    var up = StockAnalysisViewController.upcoming(PREDS);
    assertThat(up.baseDate()).isEqualTo(LocalDate.of(2026, 11, 26));
    assertThat(up.runs())
        .extracting(StockAnalysisViewController.UpcomingRun::slot)
        .containsExactly("PREMARKET", "CLOSE");
    assertThat(up.runs().get(1).d5()).isNotNull();
    assertThat(up.runs().get(0).live()).isTrue();
  }

  @Test
  void 시점을_고르면_그_시점_회차만_all_이면_전부() {
    assertThat(StockAnalysisViewController.ofSlot(PREDS, "CLOSE")).hasSize(3);
    assertThat(StockAnalysisViewController.ofSlot(PREDS, "all")).hasSize(PREDS.size());
  }
}
