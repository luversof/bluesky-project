package net.luversof.api.stock.service.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.domain.StockNews;
import net.luversof.api.stock.domain.StockPrediction;
import net.luversof.api.stock.domain.StockPredictionGrade;
import net.luversof.api.stock.domain.StockPriceHistory;
import net.luversof.api.stock.service.analysis.PredictionMath.Bar;
import net.luversof.api.stock.service.analysis.PredictionMath.Direction;
import net.luversof.api.stock.service.analysis.PredictionMath.Indicators;

/** 시황 분석의 채점 규칙 · 지표 · 수집 파싱. DB 없이 돈다. */
class PredictionMathTest {

  @Test
  void flatBandBoundaryIsFlat() {
    BigDecimal band = PredictionMath.flatBandPct(1);
    assertThat(PredictionMath.classify(new BigDecimal("0.5"), band)).isEqualTo(Direction.FLAT);
    assertThat(PredictionMath.classify(new BigDecimal("-0.5"), band)).isEqualTo(Direction.FLAT);
    assertThat(PredictionMath.classify(new BigDecimal("0.5001"), band)).isEqualTo(Direction.UP);
    assertThat(PredictionMath.classify(new BigDecimal("-0.5001"), band)).isEqualTo(Direction.DOWN);
    assertThat(PredictionMath.flatBandPct(5)).isEqualByComparingTo("1.5");
  }

  @Test
  void lateMeansCreatedAtOrAfterNextOpen() {
    LocalDate next = LocalDate.of(2026, 11, 10);
    assertThat(PredictionMath.isLate(Instant.parse("2026-11-09T23:59:59Z"), next)).isFalse();
    // 2026-11-10 09:00 KST = 00:00Z
    assertThat(PredictionMath.isLate(Instant.parse("2026-11-10T00:00:00Z"), next)).isTrue();
  }

  @Test
  void indicators() {
    List<Bar> bars = new ArrayList<>();
    for (int i = 0; i < 21; i++) {
      // 종가 100..120, 거래량 1000, 마지막 날만 3000
      bars.add(
          new Bar(
              LocalDate.of(2026, 10, 1).plusDays(i),
              BigDecimal.valueOf(100 + i),
              i == 20 ? 3000 : 1000));
    }
    Indicators ind = PredictionMath.indicators(bars);
    assertThat(ind.change1dPct()).isEqualByComparingTo("0.8403"); // 119 -> 120
    assertThat(ind.change20dPct()).isEqualByComparingTo("20"); // 100 -> 120
    assertThat(ind.ma5()).isEqualByComparingTo("118");
    assertThat(ind.ma20()).isEqualByComparingTo("110.5");
    assertThat(ind.volumeRatio20()).isEqualByComparingTo("3");

    Indicators few = PredictionMath.indicators(bars.subList(0, 3));
    assertThat(few.change5dPct()).isNull();
    assertThat(few.ma20()).isNull();
    assertThat(PredictionMath.indicators(List.of()).change1dPct()).isNull();
  }

  @Test
  void gradeOfUsesStoredBasePriceAndBaselines() {
    StockPrediction p = new StockPrediction();
    p.setId(UUID.randomUUID());
    p.setBaseDate(LocalDate.of(2026, 11, 6));
    p.setBasePrice(new BigDecimal("100"));
    p.setHorizonDays(1);
    p.setDirection("UP");
    p.setCreatedDate(Instant.parse("2026-11-08T23:00:00Z")); // 11-09 08:00 KST

    StockPredictionGrade g =
        StockAnalysisService.gradeOf(
            p,
            row(LocalDate.of(2026, 11, 9), "101"),
            LocalDate.of(2026, 11, 9),
            List.of(row(LocalDate.of(2026, 11, 6), "100"), row(LocalDate.of(2026, 11, 5), "102")));

    assertThat(g.getActualChangePct()).isEqualByComparingTo("1");
    assertThat(g.getActualDirection()).isEqualTo("UP");
    assertThat(g.isHit()).isTrue();
    assertThat(g.isLate()).isFalse();
    assertThat(g.isAlwaysUpHit()).isTrue();
    // 기준일 하루 움직임은 102 -> 100 (DOWN), 실제는 UP
    assertThat(g.getPrevDirectionHit()).isFalse();
  }

  @Test
  void validateRejectsBadInput() {
    StockItem item = new StockItem();
    item.setSymbol("005930");
    Map<String, StockItem> targets = Map.of("005930", item);
    assertThat(
            StockAnalysisService.validate(
                new StockAnalysisService.PredictionInput("005930", 1, "UP", 70, "r"), targets))
        .isNull();
    assertThat(
            StockAnalysisService.validate(
                new StockAnalysisService.PredictionInput("000000", 1, "UP", 70, "r"), targets))
        .contains("대상");
    assertThat(
            StockAnalysisService.validate(
                new StockAnalysisService.PredictionInput("005930", 3, "UP", 70, "r"), targets))
        .contains("horizonDays");
    assertThat(
            StockAnalysisService.validate(
                new StockAnalysisService.PredictionInput("005930", 1, "up", 70, "r"), targets))
        .contains("direction");
    assertThat(
            StockAnalysisService.validate(
                new StockAnalysisService.PredictionInput("005930", 1, "UP", 101, "r"), targets))
        .contains("confidence");
  }

  @Test
  void ratePctAndBucket() {
    assertThat(StockAnalysisService.ratePct(List.of(true, false, true), b -> b))
        .isEqualByComparingTo("66.7");
    assertThat(StockAnalysisService.ratePct(List.<Boolean>of(), b -> b)).isNull();
    assertThat(StockAnalysisService.confidenceBucket(85)).isEqualTo("85-100");
    assertThat(StockAnalysisService.confidenceBucket(49)).isEqualTo("0-49");
  }

  @Test
  void naverClusterParsing() {
    Map<String, Object> first =
        Map.of(
            "officeId", "119",
            "articleId", "0003153103",
            "officeName", "데일리안",
            "datetime", "202611091740",
            "title", "월급날이 두 번? &quot;커버드콜&quot;",
            "body", "본문 앞부분");
    Map<String, Object> second =
        Map.of("officeId", "011", "articleId", "1", "datetime", "202611091114", "title", "같은 묶음");
    Map<String, Object> body =
        Map.of(
            "clusters",
            List.of(
                Map.of("items", List.of(first, second)),
                Map.of("items", List.of()),
                Map.of("items", List.of(Map.of("title", "필수값 없음")))));

    List<StockNews> news = StockNewsCollectService.naverClustersToNews(UUID.randomUUID(), body);

    // 묶음마다 대표 하나, 빈 묶음 · 깨진 기사는 건너뛴다
    assertThat(news).hasSize(1);
    StockNews n = news.get(0);
    assertThat(n.getTitle()).isEqualTo("월급날이 두 번? \"커버드콜\"");
    assertThat(n.getExternalId()).isEqualTo("119/0003153103");
    assertThat(n.getUrl()).isEqualTo("https://n.news.naver.com/mnews/article/119/0003153103");
    assertThat(n.getSummary()).isEqualTo("[데일리안] 본문 앞부분");
    // 17:40 KST = 08:40Z
    assertThat(n.getPublishedDate()).isEqualTo(Instant.parse("2026-11-09T08:40:00Z"));
    assertThat(StockNewsCollectService.naverClustersToNews(UUID.randomUUID(), Map.of())).isEmpty();
  }

  @Test
  void dartItemParsing() {
    StockNews news =
        StockNewsCollectService.dartToNews(
            UUID.randomUUID(),
            Map.of(
                "rcept_no",
                "20261109000123",
                "report_nm",
                "주요사항보고서",
                "flr_nm",
                "삼성전자",
                "rcept_dt",
                "20261109"));
    assertThat(news.getUrl()).endsWith("rcpNo=20261109000123");
    assertThat(news.getPublishedDate()).isEqualTo(Instant.parse("2026-11-08T15:00:00Z"));
  }

  private static StockPriceHistory row(LocalDate date, String close) {
    StockPriceHistory row = new StockPriceHistory();
    row.setTradeDate(date);
    row.setClosePrice(new BigDecimal(close));
    return row;
  }
}
