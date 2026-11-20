package net.luversof.web.gate.stock.httpexchange;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
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
  Summary summary(@RequestParam(required = false) String symbol);

  /** 한 종목의 일자별 예측과 버전별 적중률. */
  @GetExchange("/history")
  SymbolHistory history(@RequestParam String symbol);

  record SymbolHistory(
      String symbol, String name, List<RateRow> byVersion, List<RecentPrediction> predictions) {}

  /** 버전 기록: 바꾼 것 · 리플레이 결과 · 판단과 다음 개선 후보. */
  @GetExchange("/releases")
  ReleaseNotes releases();

  record ReleaseNotes(List<Release> releases, List<NextStep> next) {}

  record Release(
      LocalDate date, String version, String area, String change, String result, String verdict) {}

  record NextStep(String title, String judgment, String priority) {}

  @GetExchange("/runs")
  RunComparison runs(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(required = false) String symbol);

  record RunInfo(
      UUID runId,
      int seq,
      Instant createdDate,
      String promptVersion,
      String modelId,
      String replay,
      int predictions,
      int graded,
      BigDecimal hitRatePct,
      BigDecimal flatPct) {}

  record RunCell(
      String direction,
      int confidence,
      String rationale,
      LocalDate baseDate,
      Boolean hit,
      String actualDirection,
      BigDecimal actualChangePct) {}

  record RunRow(
      String symbol,
      String name,
      int horizonDays,
      java.util.Map<String, RunCell> cells,
      boolean disagree) {}

  record RunComparison(
      LocalDate date, List<LocalDate> dates, List<RunInfo> runs, List<RunRow> rows) {}

  @GetExchange("/predictions")
  List<RecentPrediction> recent(@RequestParam int days);

  /** 기준일의 채점 전 예측 삭제(채점된 것은 남긴다). */
  @DeleteExchange("/predictions")
  DeleteResult deletePredictions(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate);

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
      BigDecimal edgePct,
      // Integer: 이 필드가 없는 예전 api-stock 응답도 읽는다(Jackson 3 은 없는 int 를 실패로 본다)
      Integer reanalysisGraded) {}

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
      String replay,
      Instant createdDate,
      int runSeq,
      LocalDate targetDate,
      BigDecimal actualChangePct,
      String actualDirection,
      Boolean hit,
      Boolean late) {}
}
