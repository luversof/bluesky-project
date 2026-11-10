package net.luversof.web.gate.stock.httpexchange;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/** 시황 분석(api-stock /api/stock/analysis) 조회. 응답 모양은 api-stock StockAnalysisService 의 record 와 같다. */
@HttpExchange(
    url = "/api/stock/analysis",
    contentType = MediaType.APPLICATION_JSON_VALUE,
    accept = MediaType.APPLICATION_JSON_VALUE)
public interface StockAnalysisClient {

  /** userId: KIS 키 주인 - 시장 상황(market)을 KIS 로 가져온다. */
  @GetExchange("/input")
  AnalysisInput input(@RequestParam UUID userId);

  @GetExchange("/summary")
  Summary summary();

  @GetExchange("/predictions")
  List<RecentPrediction> recent(@RequestParam int days);

  /** 기준일의 채점 전 예측 삭제(채점된 것은 남긴다). */
  @DeleteExchange("/predictions")
  DeleteResult deletePredictions(@RequestParam LocalDate baseDate);

  record DeleteResult(int deleted, int keptGraded) {}

  record Indicators(
      BigDecimal change1dPct,
      BigDecimal change5dPct,
      BigDecimal change20dPct,
      BigDecimal ma5,
      BigDecimal ma20,
      BigDecimal closeVsMa20Pct,
      BigDecimal volumeRatio20) {}

  record NewsLine(String source, String title, String summary, String url, Instant publishedDate) {}

  record AnalysisItem(
      String symbol,
      String name,
      LocalDate baseDate,
      BigDecimal lastClose,
      List<String> tags,
      Indicators indicators,
      List<BigDecimal> recentCloses,
      List<NewsLine> news) {}

  record MarketLine(
      String key,
      String name,
      LocalDate lastDate,
      BigDecimal close,
      BigDecimal change1dPct,
      BigDecimal change5dPct) {}

  record MarketContext(Instant fetchedAt, List<MarketLine> lines, List<String> unavailable) {}

  record AnalysisInput(Instant generatedAt, MarketContext market, List<AnalysisItem> items) {}

  record RateRow(
      String group,
      int horizonDays,
      int graded,
      BigDecimal hitRatePct,
      BigDecimal alwaysUpRatePct,
      BigDecimal trendRatePct,
      BigDecimal majorityRatePct,
      BigDecimal edgePct) {}

  record Summary(
      int predictions,
      int pending,
      int late,
      List<RateRow> byHorizon,
      List<RateRow> byConfidence,
      List<RateRow> byPromptVersion,
      List<RateRow> byRun) {}

  record RecentPrediction(
      String symbol,
      String name,
      LocalDate baseDate,
      int horizonDays,
      String direction,
      int confidence,
      String rationale,
      BigDecimal basePrice,
      String promptVersion,
      Instant createdDate,
      LocalDate targetDate,
      BigDecimal actualChangePct,
      String actualDirection,
      Boolean hit,
      Boolean late) {}
}
