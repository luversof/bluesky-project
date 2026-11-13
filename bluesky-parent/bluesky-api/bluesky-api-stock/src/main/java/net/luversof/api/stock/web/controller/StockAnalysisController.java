package net.luversof.api.stock.web.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.stock.service.analysis.MarketDailyCollectService;
import net.luversof.api.stock.service.analysis.StockAnalysisService;
import net.luversof.api.stock.service.analysis.StockNewsCollectService;

/**
 * 시황 분석. 작업 스케줄러 스크립트(tools/market-analysis)가 아침 · 저녁에 부른다.
 *
 * <p>아침: 시세 갱신(/api/stock/admin/price-histories) → news → input → (claude -p) → predictions. 저녁: 시세
 * 갱신 → news → grades.
 */
@RestController
@RequestMapping("/api/stock/analysis")
public class StockAnalysisController {

  @Autowired private StockAnalysisService stockAnalysisService;

  @Autowired private StockNewsCollectService stockNewsCollectService;

  @Autowired private MarketDailyCollectService marketDailyCollectService;

  /** 시장 일별 시세 최근분(KOSPI200 선물 주간 · 야간 종가, 지난 2 주). 아침 · 저녁 작업이 부른다. */
  @PostMapping("/market-daily")
  public MarketDailyCollectService.CollectResult collectMarketDaily(@RequestParam UUID userId) {
    return marketDailyCollectService.collectRecent(userId);
  }

  /** 시장 일별 시세 되채우기(2022-06 부터 가능). */
  @PostMapping("/market-daily/backfill")
  public MarketDailyCollectService.CollectResult backfillMarketDaily(
      @RequestParam UUID userId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return marketDailyCollectService.backfill(userId, from, to);
  }

  @PostMapping("/news")
  public StockNewsCollectService.CollectResult collectNews(@RequestParam UUID userId) {
    return stockNewsCollectService.collect(userId, stockAnalysisService.findTargets());
  }

  /** 분석 대상 추가: 없는 종목은 만들고 analysis 태그를 붙인다. */
  @PostMapping("/targets")
  public StockAnalysisService.TargetResult addTargets(
      @RequestBody List<StockAnalysisService.TargetInput> targets) {
    return stockAnalysisService.addTargets(targets);
  }

  /** 과거 뉴스 되채우기: 종목 하나를 from 까지(쪽 수 상한 maxPages). 리플레이가 과거 기준일에도 뉴스를 보게 한다. */
  @PostMapping("/news/backfill")
  public StockNewsCollectService.BackfillResult backfillNews(
      @RequestParam String symbol,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(defaultValue = "300") int maxPages) {
    return stockNewsCollectService.backfill(
        stockAnalysisService.findTargets().stream()
            .filter(i -> i.getSymbol().equals(symbol))
            .findFirst()
            .orElseThrow(() ->
                new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND,
                    "not an analysis target: " + symbol)),
        from,
        Math.clamp(maxPages, 1, 2000));
  }

  @GetMapping("/input")
  public StockAnalysisService.AnalysisInput input(
      @RequestParam(required = false) UUID userId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf,
      @RequestParam(defaultValue = "false") boolean morning,
      @RequestParam(defaultValue = "false") boolean flows,
      @RequestParam(defaultValue = "-1") int chunk,
      @RequestParam(defaultValue = "10") int chunkSize) {
    StockAnalysisService.AnalysisInput input =
        stockAnalysisService.buildInput(userId, asOf, morning, flows);
    if (chunk < 0) {
      return input;
    }
    // 대상이 많으면(40 종목, 2026-11-11) 한 번의 claude 호출에 다 넣지 않고 나눠 부른다 - 스크립트가 chunk 0, 1, ... 을 빈 목록이 올
    // 때까지 부른다.
    int size = Math.clamp(chunkSize, 1, 100);
    int from = Math.min(chunk * size, input.items().size());
    return new StockAnalysisService.AnalysisInput(
        input.generatedAt(),
        input.market(),
        input.items().subList(from, Math.min(from + size, input.items().size())),
        input.investorFlows());
  }

  /** 리플레이: 지난 기준일 asOf 의 예측을 저장하지 않고 채점한다(tools/market-analysis/replay.py). */
  @PostMapping("/evaluate")
  public StockAnalysisService.EvalResult evaluate(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf,
      @RequestBody StockAnalysisService.SubmitRequest request) {
    return stockAnalysisService.evaluate(asOf, request);
  }

  @PostMapping("/predictions")
  public StockAnalysisService.SubmitResult submit(
      @RequestBody StockAnalysisService.SubmitRequest request) {
    return stockAnalysisService.submit(request);
  }

  @GetMapping("/predictions")
  public List<StockAnalysisService.RecentPrediction> recent(
      @RequestParam(defaultValue = "14") int days) {
    return stockAnalysisService.recent(Math.clamp(days, 1, 365));
  }

  /** 관리 화면: 기준일의 채점 전 예측 삭제. */
  @DeleteMapping("/predictions")
  public StockAnalysisService.DeleteResult deletePredictions(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
    return stockAnalysisService.deleteUngraded(baseDate);
  }

  @PostMapping("/grades")
  public StockAnalysisService.GradeResult grade() {
    return stockAnalysisService.grade();
  }

  /** 분석 날짜(KST) 하루의 회차들을 종목 · 기간별로 나란히. date 를 빼면 가장 최근 날짜. */
  @GetMapping("/runs")
  public StockAnalysisService.RunComparison runs(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate date) {
    return stockAnalysisService.runs(date);
  }

  @GetMapping("/summary")
  public StockAnalysisService.Summary summary() {
    return stockAnalysisService.summary();
  }
}
