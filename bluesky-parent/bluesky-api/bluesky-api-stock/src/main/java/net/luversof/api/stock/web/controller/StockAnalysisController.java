package net.luversof.api.stock.web.controller;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import net.luversof.api.stock.service.analysis.MarketDailyCollectService;
import net.luversof.api.stock.service.analysis.StockAnalysisService;
import net.luversof.api.stock.service.analysis.StockNewsCollectService;
import tools.jackson.databind.json.JsonMapper;

/**
 * 시황 분석
 *
 * <p>작업 스케줄러 스크립트(tools/market-analysis)가 아침 · 저녁에 부른다.
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

  @Autowired private JsonMapper jsonMapper;

  /**
   * 시장 일별 시세 수집
   *
   * <p>KOSPI200 선물 주간 · 야간 종가, 지난 2 주. 아침 · 저녁 작업이 부른다.
   */
  @PostMapping("/market-daily")
  public MarketDailyCollectService.CollectResult collectMarketDaily(@RequestParam UUID userId) {
    return marketDailyCollectService.collectRecent(userId);
  }

  /**
   * 시장 일별 시세 되채우기
   *
   * <p>2022-06 부터 가능.
   */
  @PostMapping("/market-daily/backfill")
  public MarketDailyCollectService.CollectResult backfillMarketDaily(
      @RequestParam UUID userId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return marketDailyCollectService.backfill(userId, from, to);
  }

  /**
   * 뉴스 수집
   *
   * <p>symbols 를 주면 그 종목 뉴스만 모은다(2026-11-20: 삼성전자만 분석하는데 40 종목 뉴스를 계속 모았다).
   */
  @PostMapping("/news")
  public StockNewsCollectService.CollectResult collectNews(
      @RequestParam UUID userId, @RequestParam(required = false) List<String> symbols) {
    return stockNewsCollectService.collect(
        userId,
        stockAnalysisService.findTargets().stream()
            .filter(i -> symbols == null || symbols.isEmpty() || symbols.contains(i.getSymbol()))
            .toList());
  }

  /**
   * 분석 대상 추가
   *
   * <p>없는 종목은 만들고 analysis 태그를 붙인다.
   */
  @PostMapping("/targets")
  public StockAnalysisService.TargetResult addTargets(
      @RequestBody List<StockAnalysisService.TargetInput> targets) {
    return stockAnalysisService.addTargets(targets);
  }

  /**
   * 과거 뉴스 되채우기
   *
   * <p>종목 하나를 from 까지(쪽 수 상한 maxPages). 리플레이가 과거 기준일에도 뉴스를 보게 한다.
   */
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

  /**
   * 분석 입력 생성
   *
   * <p>종목별 시세 · 지표 · 뉴스 · 시장 상황. 예약 작업과 리플레이 스크립트가 부른다.
   *
   * @param userId KIS 설정을 읽을 사용자(선택, 없으면 시장 지표를 비운다)
   * @param asOf 리플레이 기준일(선택, 그날까지의 데이터만 쓴다). 비우면 지금
   * @param morning true 면 기준일 다음 날 아침 분석 기준
   * @param flows true 면 투자자 수급을 붙인다
   * @param chunk 0 부터의 묶음 번호. 음수면 전체
   * @param chunkSize 묶음 크기
   * @return 분석 입력
   */
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

  /**
   * 리플레이 채점
   *
   * <p>지난 기준일 asOf 의 예측을 저장하지 않고 채점한다(tools/market-analysis/replay.py).
   */
  @PostMapping("/evaluate")
  public StockAnalysisService.EvalResult evaluate(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf,
      @RequestBody StockAnalysisService.SubmitRequest request) {
    return stockAnalysisService.evaluate(asOf, request);
  }

  /**
   * 규칙 예측 조회
   *
   * <p>night = 입력의 K200NF change1dPct, premarket = 입력의 NXT_종목 change1dPct(v10), symbols = 분석할 종목.
   * 돌려준 종목은 Claude 를 부르지 않고 이 예측을 쓴다(아침 작업 · replay.py). 빈 목록이면 전부 Claude 가 분석한다.
   */
  @GetMapping("/rule-predictions")
  public List<StockAnalysisService.PredictionInput> rulePredictions(
      @RequestParam(required = false) java.math.BigDecimal night,
      @RequestParam(required = false) java.math.BigDecimal premarket,
      @RequestParam List<String> symbols) {
    return StockAnalysisService.rulePredictions(night, premarket, symbols);
  }

  /**
   * 예측이 없는 기준일 조회
   *
   * <p>최근 lookback 거래일 중 예측이 없는 기준일(아침 작업이 채운다). version 을 주면 그 버전 예측이 없는 기준일(재분석할 날).
   */
  @GetMapping("/missing-dates")
  public List<LocalDate> missingDates(
      @RequestParam List<String> symbols,
      @RequestParam(defaultValue = "5") int lookback,
      @RequestParam(required = false) String version) {
    return stockAnalysisService.missingDates(symbols, Math.clamp(lookback, 1, 60), version);
  }

  /**
   * 예측 저장
   *
   * <p>replay · asOf 를 주면 리플레이 회차로 저장하고 바로 채점한다(replay.py --save).
   */
  @PostMapping("/predictions")
  public StockAnalysisService.SubmitResult submit(
      @RequestBody StockAnalysisService.SubmitRequest request,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf,
      @RequestParam(defaultValue = "false") boolean morning,
      @RequestParam(required = false) String replay) {
    return stockAnalysisService.submit(request, asOf, morning, replay);
  }

  /**
   * 최근 예측 목록 조회
   *
   * @param days 최근 며칠(1~365, 기본 14)
   * @return 예측과 채점 결과
   */
  @GetMapping("/predictions")
  public List<StockAnalysisService.RecentPrediction> recent(
      @RequestParam(defaultValue = "14") int days) {
    return stockAnalysisService.recent(Math.clamp(days, 1, 365));
  }

  /**
   * 채점 전 예측 삭제
   *
   * <p>관리 화면에서 기준일의 채점 전 예측을 지운다.
   */
  @DeleteMapping("/predictions")
  public StockAnalysisService.DeleteResult deletePredictions(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
    return stockAnalysisService.deleteUngraded(baseDate);
  }

  /**
   * 예측 채점
   *
   * <p>regradeDays: 만기일이 최근 이 일수 안인 채점은 종가가 바뀌었으면 다시 매긴다(기본 10, 지난 시세를 바로잡은 뒤 한 번 크게).
   */
  @PostMapping("/grades")
  public StockAnalysisService.GradeResult grade(
      @RequestParam(defaultValue = "" + StockAnalysisService.REGRADE_DAYS) int regradeDays) {
    return stockAnalysisService.grade(Math.clamp(regradeDays, 0, 800));
  }

  /**
   * 하루 회차 비교 조회
   *
   * <p>분석 날짜(KST) 하루의 회차들을 종목 · 기간별로 나란히. date 를 빼면 가장 최근 날짜.
   */
  @GetMapping("/runs")
  public StockAnalysisService.RunComparison runs(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(required = false) String symbol) {
    return stockAnalysisService.runs(date, symbol);
  }

  /**
   * 종목 예측 이력 조회
   *
   * <p>한 종목의 일자별 예측과 버전별 적중률.
   */
  @GetMapping("/history")
  public StockAnalysisService.SymbolHistory history(@RequestParam String symbol) {
    return stockAnalysisService.history(symbol);
  }

  /**
   * 적중률 요약 조회
   *
   * <p>늦은 예측(장중 정보가 섞인 것)은 뺀다.
   *
   * @param symbol 종목 심볼(선택). 주면 그 종목 예측만으로 센다
   * @return 회차 · 기준선별 적중률
   */
  @GetMapping("/summary")
  public StockAnalysisService.Summary summary(@RequestParam(required = false) String symbol) {
    return stockAnalysisService.summary(symbol);
  }

  /**
   * 버전 기록 조회
   *
   * <p>바꾼 것 · 리플레이 결과 · 판단과 다음 개선 후보(resources/analysis/releases.json).
   */
  @GetMapping("/releases")
  public ReleaseNotes releases() throws IOException {
    try (InputStream in = new ClassPathResource("analysis/releases.json").getInputStream()) {
      return jsonMapper.readValue(in, ReleaseNotes.class);
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ReleaseNotes(List<Release> releases, List<NextStep> next) {}

  public record Release(
      LocalDate date, String version, String area, String change, String result, String verdict) {}

  public record NextStep(String title, String judgment, String priority) {}
}
