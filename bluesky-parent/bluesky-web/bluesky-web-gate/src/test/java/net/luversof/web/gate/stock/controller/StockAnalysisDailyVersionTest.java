package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.httpexchange.StockAnalysisClient;

/** 일자별 적중을 버전별로(사용자 요청 2026-11-22): 기본은 가장 최근 채택 버전. */
class StockAnalysisDailyVersionTest {

  private static StockAnalysisClient.RecentPrediction pred(
      String version, String replay, String baseDate, String created) {
    return new StockAnalysisClient.RecentPrediction(
        "005930",
        "삼성전자",
        LocalDate.parse(baseDate),
        1,
        "UP",
        60,
        null,
        null,
        version,
        replay,
        Instant.parse(created),
        1,
        null,
        null,
        null,
        true,
        false);
  }

  private static final List<StockAnalysisClient.RecentPrediction> PREDS =
      List.of(
          // 11-10: 그날 운영은 v3, v10 은 재분석
          pred("v3", null, "2026-11-10", "2026-11-10T23:00:00Z"),
          // 재분석은 만든 시각이 예전 날짜로 찍힌다 - 그래도 v10 이 v3 보다 앞
          pred("v10", "재분석 v10", "2026-11-10", "2026-11-10T10:00:00Z"),
          // 11-20: v10 운영과 v10 재분석 - 운영이 먼저
          pred("v10", "재분석 v10", "2026-11-20", "2026-11-22T00:01:00Z"),
          pred("v10", null, "2026-11-20", "2026-11-20T23:30:00Z"));

  @Test
  void 버전을_고르면_그_버전_회차만_운영_우선은_예전처럼_섞인다() {
    var v10 = StockAnalysisViewController.dailyRows(PREDS, "v10", null);
    assertThat(v10).hasSize(2);
    assertThat(v10.get(0).d1().replay()).isNull(); // 11-20 운영 v10
    assertThat(v10.get(1).d1().replay()).isEqualTo("재분석 v10"); // 11-10 은 재분석

    var live = StockAnalysisViewController.dailyRows(PREDS, "live", null);
    assertThat(live.get(1).d1().promptVersion()).isEqualTo("v3");
  }

  private static StockAnalysisClient.RecentPrediction conf(int confidence, boolean hit) {
    return new StockAnalysisClient.RecentPrediction(
        "005930",
        "삼성전자",
        LocalDate.of(2026, 11, 10),
        1,
        "UP",
        confidence,
        null,
        null,
        "v10",
        null,
        Instant.EPOCH,
        1,
        null,
        null,
        null,
        hit,
        false);
  }

  @Test
  void 확신도_기준_이상인_날만_세고_그런_날이_없는_기준은_뺀다() {
    var rows =
        List.of(
            new StockAnalysisViewController.DailyRow(
                LocalDate.of(2026, 11, 12), conf(80, true), null),
            new StockAnalysisViewController.DailyRow(
                LocalDate.of(2026, 11, 11), conf(57, false), null),
            new StockAnalysisViewController.DailyRow(
                LocalDate.of(2026, 11, 10), conf(40, false), null));
    var cuts = StockAnalysisViewController.confidenceCuts(rows);
    assertThat(cuts)
        .extracting(StockAnalysisViewController.ConfidenceCut::min)
        .containsExactly(0, 50, 60, 70, 80);
    assertThat(cuts.get(0).hits().d1Graded()).isEqualTo(3);
    assertThat(cuts.get(1).hits().d1Graded()).isEqualTo(2); // 50 이상: 80 · 57
    assertThat(cuts.get(4).hits().d1Hit()).isEqualTo(1);
    assertThat(cuts.get(4).hits().d1RatePct()).isEqualByComparingTo("100.0");

    var low = StockAnalysisViewController.confidenceCuts(List.of(rows.get(2)));
    assertThat(low).extracting(StockAnalysisViewController.ConfidenceCut::min).containsExactly(0);
  }

  @Test
  void 최근_채택은_버전_기록에서_첫_채택_줄의_첫_낱말() {
    var notes =
        new StockAnalysisClient.ReleaseNotes(
            List.of(
                new StockAnalysisClient.Release(null, "여러 LLM 앙상블", null, "", null, "REJECTED"),
                new StockAnalysisClient.Release(null, "v10 확신도 보정", null, "", null, "ADOPTED"),
                new StockAnalysisClient.Release(null, "v9", null, "", null, "ADOPTED")),
            List.of());
    var versions = StockAnalysisViewController.versionKeys(PREDS);
    assertThat(versions).containsExactly("v10", "v3");
    assertThat(StockAnalysisViewController.latestAdopted(notes, versions)).isEqualTo("v10");
    // 채택 버전의 예측이 없는 종목은 null - 화면은 운영 우선으로 보여 준다
    assertThat(StockAnalysisViewController.latestAdopted(notes, List.of("v3"))).isNull();
    assertThat(StockAnalysisViewController.latestAdopted(null, versions)).isNull();
  }
}
