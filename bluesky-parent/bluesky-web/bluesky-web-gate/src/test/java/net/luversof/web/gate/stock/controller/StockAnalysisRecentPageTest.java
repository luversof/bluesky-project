package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;

import net.luversof.web.gate.stock.httpexchange.StockAnalysisClient;

/** 시황 분석 최근 예측 쪽 나누기(사용자 요청 2026-11-15). */
class StockAnalysisRecentPageTest {

  private static final List<Integer> ROWS = IntStream.range(0, 45).boxed().toList();

  @Test
  void 한_쪽은_20_줄이고_마지막_쪽은_남은_줄만() {
    Page<Integer> first = StockAnalysisViewController.pageOf(ROWS, 1);
    Page<Integer> last = StockAnalysisViewController.pageOf(ROWS, 3);
    assertThat(first.getContent()).hasSize(20).startsWith(0);
    assertThat(first.getTotalPages()).isEqualTo(3);
    assertThat(last.getContent()).containsExactly(40, 41, 42, 43, 44);
  }

  @Test
  void 범위를_벗어난_쪽_번호는_가장_가까운_쪽() {
    assertThat(StockAnalysisViewController.pageOf(ROWS, 0).getNumber()).isZero();
    assertThat(StockAnalysisViewController.pageOf(ROWS, 99).getNumber()).isEqualTo(2);
    assertThat(StockAnalysisViewController.pageOf(List.of(), 1).getContent()).isEmpty();
  }

  private static net.luversof.web.gate.stock.httpexchange.StockAnalysisClient.AnalysisItem item(
      String symbol) {
    return new net.luversof.web.gate.stock.httpexchange.StockAnalysisClient.AnalysisItem(
        symbol, symbol, null, null, null, null, null, null);
  }

  @Test
  void 종목별_예측은_고른_종목_없으면_삼성전자_그것도_없으면_첫_대상() {
    var items = List.of(item("000660"), item("005930"));
    assertThat(StockAnalysisViewController.historySymbolOf("000660", items)).isEqualTo("000660");
    assertThat(StockAnalysisViewController.historySymbolOf("999999", items)).isEqualTo("005930");
    assertThat(StockAnalysisViewController.historySymbolOf(null, List.of(item("000660"))))
        .isEqualTo("000660");
    assertThat(StockAnalysisViewController.historySymbolOf(null, List.of())).isNull();
  }

  private static net.luversof.web.gate.stock.httpexchange.StockAnalysisClient.AnalysisItem item(
      String symbol, String name) {
    return new net.luversof.web.gate.stock.httpexchange.StockAnalysisClient.AnalysisItem(
        symbol, name, null, null, null, null, null, null);
  }

  @Test
  void 검색이_한_종목만_가리킬_때만_그_종목_기준() {
    var items = List.of(item("005930", "삼성전자"), item("005935", "삼성전자우"), item("000660", "SK하이닉스"));
    assertThat(StockAnalysisViewController.focusOf("하이닉스", items).symbol()).isEqualTo("000660");
    // 여러 종목에 맞으면 코드가 정확히 같은 것만, 그것도 없으면 전체 기준
    assertThat(StockAnalysisViewController.focusOf("005930", items).symbol()).isEqualTo("005930");
    assertThat(StockAnalysisViewController.focusOf("삼성전자", items)).isNull();
    assertThat(StockAnalysisViewController.focusOf("", items)).isNull();
  }

  private static StockAnalysisClient.RecentPrediction pred(
      String baseDate,
      int horizon,
      String created,
      int seq,
      String replay,
      Boolean hit,
      Boolean late) {
    return new StockAnalysisClient.RecentPrediction(
        "005930",
        "삼성전자",
        LocalDate.parse(baseDate),
        horizon,
        "UP",
        60,
        null,
        null,
        "v9",
        replay,
        Instant.parse(created),
        seq,
        null,
        null,
        null,
        hit,
        late,
        "PREMARKET");
  }

  @Test
  void 일자별_적중은_기준일마다_운영_회차를_먼저_고르고_늦은_예측은_뺀다() {
    var preds =
        List.of(
            // 11-17: 운영 회차(실패)와 나중 리플레이(적중) - 운영 회차를 쓴다
            pred("2026-11-17", 1, "2026-11-17T23:00:00Z", 1, null, false, false),
            pred(
                "2026-11-17", 5, "2026-11-17T23:00:00.022Z", 1, null, null, false), // 같은 회차, 밀리초 차이
            pred("2026-11-17", 1, "2026-11-18T05:00:00Z", 2, "누락채움", true, false),
            // 11-16: 리플레이만 - 같은 시각 두 회차 중 번호가 큰 것
            pred("2026-11-16", 1, "2026-11-16T23:00:00Z", 1, "A", false, false),
            pred("2026-11-16", 1, "2026-11-16T23:00:00Z", 2, "B", true, false),
            // 11-13: 늦은 예측뿐 - 줄이 없다
            pred("2026-11-13", 1, "2026-11-14T03:00:00Z", 1, null, true, true),
            // 기간 밖
            pred("2026-10-01", 1, "2026-10-01T23:00:00Z", 1, null, true, false));
    var rows = StockAnalysisViewController.dailyRows(preds, LocalDate.of(2026, 11, 1));
    assertThat(rows)
        .extracting(StockAnalysisViewController.DailyRow::baseDate)
        .containsExactly(LocalDate.of(2026, 11, 17), LocalDate.of(2026, 11, 16));
    assertThat(rows.get(0).d1().replay()).isNull();
    assertThat(rows.get(0).d5()).isNotNull();
    assertThat(rows.get(1).d1().replay()).isEqualTo("B");
    assertThat(rows.get(1).d5()).isNull();

    var hits = StockAnalysisViewController.dailyHits(rows);
    assertThat(hits.d1Hit()).isEqualTo(1);
    assertThat(hits.d1Graded()).isEqualTo(2);
    assertThat(hits.d1RatePct()).isEqualByComparingTo("50.0");
    assertThat(hits.d5Graded()).isZero();
    assertThat(hits.d5RatePct()).isNull();
  }

  @Test
  void 기간은_몇_달_전부터_all_은_전부() {
    var today = LocalDate.of(2026, 11, 20);
    assertThat(StockAnalysisViewController.periodStart("3m", today))
        .isEqualTo(LocalDate.of(2026, 8, 20));
    assertThat(StockAnalysisViewController.periodStart("all", today)).isNull();
  }

  @Test
  void 실제_종가는_기준가에_변화율을_곱한다() {
    var p =
        new StockAnalysisClient.RecentPrediction(
            "005930",
            "삼성전자",
            LocalDate.of(2026, 11, 19),
            1,
            "DOWN",
            77,
            null,
            new java.math.BigDecimal("75600"),
            "rule",
            null,
            null,
            1,
            LocalDate.of(2026, 11, 20),
            new java.math.BigDecimal("-7.0086"),
            "DOWN",
            true,
            false,
            "PREMARKET");
    assertThat(StockAnalysisViewController.actualClose(p)).isEqualByComparingTo("70301");
    assertThat(
            StockAnalysisViewController.actualClose(
                pred("2026-11-19", 1, "2026-11-19T23:00:00Z", 1, null, null, false)))
        .isNull();
  }

  private static StockAnalysisClient.RecentPrediction vpred(
      String version, String replay, String baseDate, String created, Boolean hit) {
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
        hit,
        false,
        "PREMARKET");
  }

  @Test
  void 버전_비교는_새_버전을_기준으로_같은_날짜만_센다() {
    var preds =
        List.of(
            // 예전 버전 v3 운영(11-10 적중, 11-11 실패), 빠진 날 채움은 v3 로 센다(11-12)
            vpred("v3", null, "2026-11-10", "2026-11-10T23:00:00Z", true),
            vpred("v3", null, "2026-11-11", "2026-11-11T23:00:00Z", false),
            vpred("v3", "누락채움", "2026-11-12", "2026-11-13T05:00:00Z", false),
            // 규칙: 나중에 나왔어도 기준이 아니다
            vpred("rule", null, "2026-11-12", "2026-11-19T23:00:00Z", true),
            // v9 재분석: 11-10 실패, 11-11 · 11-12 적중. 규칙 날 재분석은 promptVersion 이 rule 이어도 v9 열
            vpred("v9", "재분석 v9", "2026-11-10", "2026-11-21T00:00:00Z", false),
            vpred("v9", "재분석 v9", "2026-11-11", "2026-11-21T00:01:00Z", true),
            vpred("rule", "재분석 v9", "2026-11-12", "2026-11-21T00:02:00Z", true),
            // 한 버전만 있는 날은 줄이 없다
            vpred("v9", null, "2026-11-18", "2026-11-18T23:00:00Z", null),
            // 실험 리플레이는 따로, 나중에 돌렸어도 기준이 아니다
            vpred("v9", "뉴스없음", "2026-11-10", "2026-11-20T00:00:00Z", true));
    var c = StockAnalysisViewController.versionCompare(preds, null);
    assertThat(c.versions()).containsExactly("v9", "v3", "v9 · 뉴스없음", "rule");
    assertThat(c.rows())
        .extracting(StockAnalysisViewController.CompareRow::baseDate)
        .containsExactly(
            LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 11), LocalDate.of(2026, 11, 10));
    var v3 = c.vsNewest().stream().filter(h -> h.version().equals("v3")).findFirst().orElseThrow();
    assertThat(v3.d1Days()).isEqualTo(3);
    assertThat(v3.d1Hit()).isEqualTo(1);
    assertThat(v3.d1NewestHit()).isEqualTo(2);
    assertThat(v3.d5Days()).isZero();
    // 기준과 같이 채점된 날이 있는 버전만 비교 줄이 생긴다(뉴스없음 11-10, 규칙 11-12)
    assertThat(c.vsNewest())
        .extracting(StockAnalysisViewController.PairHits::version)
        .containsExactly("v3", "v9 · 뉴스없음", "rule");
  }
}
