package net.luversof.api.stock.service.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.api.stock.domain.StockPrediction;
import net.luversof.api.stock.domain.StockPredictionGrade;
import net.luversof.api.stock.service.analysis.StockAnalysisService.Graded;

class StockAnalysisBaselineTest {

  private static Graded graded(int horizon, List<?> recentCloses, String actual, Boolean prevHit) {
    StockPrediction p = new StockPrediction();
    p.setHorizonDays(horizon);
    p.setInputSnapshot(recentCloses == null ? null : Map.of("recentCloses", recentCloses));
    StockPredictionGrade g = new StockPredictionGrade();
    g.setActualDirection(actual);
    g.setPrevDirectionHit(prevHit);
    return new Graded(p, g);
  }

  @Test
  void D5_추세_기준선은_직전_5거래일_방향이다() {
    // 직전 5 거래일 100 -> 103 (+3%, 폭 1.5% 밖) = UP. 전일은 하락이지만 D+5 는 5 일 추세를 본다.
    List<Number> closes = List.of(90, 100, 101, 102, 104, 105, 103);
    assertThat(StockAnalysisService.trendHit(graded(5, closes, "UP", false))).isTrue();
    assertThat(StockAnalysisService.trendHit(graded(5, closes, "DOWN", true))).isFalse();
  }

  @Test
  void D1_은_전일_방향이고_입력_기록이_없으면_채점값을_쓴다() {
    // 104 -> 103.6 (-0.38%, 폭 0.5% 안) = FLAT. JSON 왕복 뒤 숫자는 문자열 · 실수로도 온다.
    assertThat(StockAnalysisService.trendHit(graded(1, List.of("104", 103.6), "FLAT", false)))
        .isTrue();
    assertThat(StockAnalysisService.trendHit(graded(1, null, "UP", true))).isTrue();
    assertThat(StockAnalysisService.trendHit(graded(5, null, "UP", true))).isNull();
    assertThat(StockAnalysisService.trendHit(graded(5, List.of(1, 2, 3), "UP", null))).isNull();
  }

  @Test
  void 회차_이름은_분석_날짜_몇번째_시각_버전이고_장중_회차는_표시한다() {
    Graded g = graded(1, null, "UP", null);
    g.prediction().setCreatedDate(java.time.Instant.parse("2026-11-10T11:07:12Z"));
    g.prediction().setPromptVersion("v2");
    assertThat(StockAnalysisService.runLabel(g, 2)).isEqualTo("11-10 #2 20:07 v2");
    g.grade().setLate(true);
    assertThat(StockAnalysisService.runLabel(g, 2)).isEqualTo("11-10 #2 20:07 v2 (late)");
  }

  private static StockPrediction saved(UUID runId, String createdUtc) {
    StockPrediction p = new StockPrediction();
    p.setRunId(runId);
    p.setCreatedDate(java.time.Instant.parse(createdUtc));
    return p;
  }

  @Test
  void 회차_번호는_KST_날짜마다_1부터_시각순이다() {
    UUID a = UUID.randomUUID();
    UUID b = UUID.randomUUID();
    UUID c = UUID.randomUUID();
    Map<UUID, Integer> seqs =
        StockAnalysisService.runSeqs(
            List.of(
                saved(b, "2026-11-10T11:18:58Z"),
                saved(a, "2026-11-10T11:07:12Z"),
                saved(a, "2026-11-10T11:07:13Z"),
                // KST 11-11 00:30 - 다음 날 첫 회차
                saved(c, "2026-11-10T15:30:00Z")));
    assertThat(seqs).containsEntry(a, 1).containsEntry(b, 2).containsEntry(c, 1);
  }

  @Test
  void 최빈_방향_기준선() {
    List<Graded> list =
        List.of(
            graded(1, null, "UP", null),
            graded(1, null, "FLAT", null),
            graded(1, null, "FLAT", null));
    assertThat(StockAnalysisService.majorityRatePct(list)).isEqualByComparingTo("66.7");
    assertThat(StockAnalysisService.majorityRatePct(List.of())).isNull();
  }
}
