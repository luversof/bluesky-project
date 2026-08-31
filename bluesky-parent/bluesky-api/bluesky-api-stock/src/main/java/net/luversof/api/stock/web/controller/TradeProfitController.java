package net.luversof.api.stock.web.controller;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.stock.domain.TradeProfit;
import net.luversof.api.stock.service.TradeProfitService;
import net.luversof.api.stock.web.dto.request.TradeProfitRequest;
import net.luversof.api.stock.web.dto.response.HoldingsSnapshotItem;
import net.luversof.api.stock.web.dto.response.TradeProfitTimeSeriesPoint;
import net.luversof.api.stock.web.dto.response.TradeProfitTimeSeriesResult;

@RestController
@RequestMapping("/api/tradeProfit")
public class TradeProfitController {

  @Autowired private TradeProfitService stockProfitService;

  public void setStockProfitService(TradeProfitService stockProfitService) {
    this.stockProfitService = stockProfitService;
  }

  @GetMapping("/calculateProfit")
  public List<TradeProfit> calculateProfit(TradeProfitRequest request) {
    return stockProfitService.calculateProfit(request);
  }

  @GetMapping("/timeSeries")
  public List<TradeProfitTimeSeriesPoint> timeSeries(
      TradeProfitRequest request, String granularity) {
    // Delegate to service-level efficient aggregation
    return stockProfitService.aggregateTimeSeries(request, granularity);
  }

  /**
   * 시리즈와 기간 요약을 한 번에 반환한다. 화면이 둘을 따로 호출하면 같은 시뮬레이션이 두 번 돌기 때문이다.
   *
   * <p>{@code includeSeries=false} 면 시리즈를 비우고 요약과 연도별만 돌려준다. 요약만 쓰는 호출자가 있어서다 — 게이트의 {@code
   * /stock/asset-growth/period-return} 은 이 엔드포인트를 부른 뒤 {@code summary()} 만 꺼내 쓰고 시리즈는 버린다.
   *
   * <p>실측(사용자 실데이터, {@code granularity=DAILY}): 전체 기간이면 응답 1,655,289 바이트 중 실제로 쓰이는 요약+연도별은 8,420
   * 바이트로 <b>99.5% 가 버려진다</b>(6,442 포인트). 5 년 99.3%, 1 년 98.4% 다.
   *
   * <p>새 파라미터로 만든 이유: 기존 파라미터의 뜻을 바꾸거나 엔드포인트를 새로 내면 아직 옛 버전이 떠 있는 환경에서 400 이 난다. 모르는 쿼리 파라미터는
   * 무시되므로, 옛 서버는 지금까지처럼 전체를 돌려주고 새 서버만 줄여 보낸다.
   */
  @GetMapping("/timeSeriesWithSummary")
  public TradeProfitTimeSeriesResult timeSeriesWithSummary(
      TradeProfitRequest request,
      String granularity,
      @RequestParam(required = false, defaultValue = "true") boolean includeSeries,
      // 기간을 달/해로 쪼갠 성과. 주지 않으면 계산도 전송도 하지 않아 기존 화면은 그대로다.
      @RequestParam(required = false) String breakdown) {
    var result = stockProfitService.aggregateTimeSeriesWithSummary(request, granularity, breakdown);
    if (includeSeries || result == null) {
      return result;
    }
    return new TradeProfitTimeSeriesResult(
        List.of(), result.summary(), result.yearly(), result.breakdown());
  }

  @GetMapping("/holdingsSnapshot")
  public List<HoldingsSnapshotItem> holdingsSnapshot(
      @RequestParam UUID userId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(required = false) UUID accountId,
      @RequestParam(required = false) String timeZone) {
    return stockProfitService.getHoldingsSnapshot(userId, date, accountId, timeZone);
  }

  /**
   * 여러 날짜의 보유 스냅샷을 한 번에 조회한다.
   *
   * <p>호출자가 날짜마다 /holdingsSnapshot 을 순차 호출하면 날짜 수만큼 왕복이 발생한다(배당 분석은 기준일이 수십 개라 수 초가 걸렸다). 날짜별 결과를
   * ISO 날짜 문자열을 키로 하는 맵으로 한 번에 돌려준다.
   */
  @GetMapping("/holdingsSnapshotBatch")
  public Map<String, List<HoldingsSnapshotItem>> holdingsSnapshotBatch(
      @RequestParam UUID userId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) List<LocalDate> dates,
      @RequestParam(required = false) UUID accountId,
      @RequestParam(required = false) String timeZone) {
    Map<String, List<HoldingsSnapshotItem>> result = new LinkedHashMap<>();
    if (dates == null) {
      return result;
    }
    // 날짜마다 조회하면 날짜 수만큼 시뮬레이션이 돈다. 한 번의 시뮬레이션에서 모두 캡처한다.
    stockProfitService
        .getHoldingsSnapshotBatch(userId, dates, accountId, timeZone)
        .forEach((date, items) -> result.put(date.toString(), items));
    return result;
  }
}
