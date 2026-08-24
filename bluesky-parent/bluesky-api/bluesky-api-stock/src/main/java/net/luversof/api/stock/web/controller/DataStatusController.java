package net.luversof.api.stock.web.controller;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.stock.repository.DividendRepository;
import net.luversof.api.stock.repository.StockItemRepository;
import net.luversof.api.stock.repository.StockPriceHistoryRepository;
import net.luversof.api.stock.repository.TradeRepository;
import net.luversof.api.stock.web.dto.response.DataStatusResponse;

/** 데이터 최신 시점 조회. 관리 화면의 "언제까지의 데이터인지" 표시에 쓰는 경량 엔드포인트다. */
@RestController
@RequestMapping("/api/dataStatus")
public class DataStatusController {

  private static final ZoneId MARKET_ZONE_ID = ZoneId.of("Asia/Seoul");

  @Autowired private TradeRepository tradeRepository;

  @Autowired private DividendRepository dividendRepository;

  @Autowired private StockPriceHistoryRepository stockPriceHistoryRepository;

  @Autowired private StockItemRepository stockItemRepository;

  @Autowired
  private net.luversof.api.stock.repository.MonthlyDividendPayoutRepository
      monthlyDividendPayoutRepository;

  @GetMapping
  public DataStatusResponse findDataStatus(@RequestParam UUID userId) {
    var payouts = monthlyDividendPayoutRepository.findAllByOrderByPayDateDescRecordDateDesc();
    // 원장별로 '마지막 일자'와 '건수'를 따로 물으면 같은 조인을 두 번 훑는다. 한 번에 읽는다.
    var tradeSummary = tradeRepository.findLedgerSummaryByUserId(userId);
    var dividendSummary = dividendRepository.findLedgerSummaryByUserId(userId);
    var priceDuplicate = stockPriceHistoryRepository.findLastDateDuplicateSummary();
    return new DataStatusResponse(
        tradeSummary != null ? tradeSummary.lastDate() : null,
        tradeSummary != null ? tradeSummary.totalCount() : 0L,
        dividendSummary != null ? dividendSummary.lastDate() : null,
        dividendSummary != null ? dividendSummary.totalCount() : 0L,
        stockPriceHistoryRepository.findLastPriceDate(),
        stockItemRepository.count(),
        priceDuplicate != null ? priceDuplicate.previousTradeDate() : null,
        priceDuplicate != null ? priceDuplicate.itemCount() : 0L,
        priceDuplicate != null ? priceDuplicate.sameCloseCount() : 0L,
        priceDuplicate != null ? priceDuplicate.sameAllCount() : 0L,
        priceDuplicate != null ? priceDuplicate.zeroVolumeCount() : 0L,
        stockPriceHistoryRepository.countAllRows(),
        stockPriceHistoryRepository.countZeroVolumeRows(),
        stockPriceHistoryRepository.countZeroVolumeRowsWithChangedClose(),
        toChangedCloseRows(stockPriceHistoryRepository.findZeroVolumeRowsWithChangedClose()),
        payouts.stream()
            .map(net.luversof.api.stock.domain.MonthlyDividendPayout::getPayDate)
            .filter(Objects::nonNull)
            .max(LocalDate::compareTo)
            .orElse(null),
        overduePayouts(payouts),
        stockPriceHistoryRepository.countPriceLimitBreachRows(),
        toBreachRows(stockPriceHistoryRepository.findPriceLimitBreachRows()));
  }

  /**
   * 종목 이름을 붙인다. 행 수에 상한이 있어(최대 5) 이름 조회는 한 번의 findAllById 로 끝난다.
   *
   * <p>이름을 못 찾으면 id 를 그대로 쓴다 &mdash; 이름이 없다고 행을 숨기면 정작 이상한 데이터를 못 보게 된다.
   */
  /**
   * 지급 이력이 제 주기를 넘겨 밀린 종목.
   *
   * <p>임계값을 상수로 박지 않고 종목 자신의 과거 최대 간격과 비교한다 &mdash; 월중(17일)·월말(익월 2일)처럼 주기가 다르고 상장 초기가 불규칙해서, 고정값을
   * 쓰면 어느 쪽이든 틀린다(실측 2026-08-23: 고정 60일로 잡으면 밀린 4종목을 하나도 못 본다).
   *
   * <p>이력이 3건 미만이면 주기를 알 수 없으므로 판단하지 않는다.
   */
  private List<DataStatusResponse.MonthlyDividendPayoutOverdueRow> overduePayouts(
      Iterable<net.luversof.api.stock.domain.MonthlyDividendPayout> payouts) {
    Map<UUID, List<LocalDate>> daysByItem = new HashMap<>();
    for (var payout : payouts) {
      if (payout.getStockItemId() != null && payout.getPayDate() != null) {
        daysByItem
            .computeIfAbsent(payout.getStockItemId(), key -> new java.util.ArrayList<>())
            .add(payout.getPayDate());
      }
    }
    if (daysByItem.isEmpty()) {
      return List.of();
    }
    Map<UUID, String> names = new HashMap<>();
    stockItemRepository
        .findAllById(daysByItem.keySet())
        .forEach(item -> names.put(item.getId(), item.getName()));
    LocalDate today = LocalDate.now(MARKET_ZONE_ID);
    List<DataStatusResponse.MonthlyDividendPayoutOverdueRow> overdue = new java.util.ArrayList<>();
    for (var entry : daysByItem.entrySet()) {
      List<LocalDate> days = entry.getValue().stream().distinct().sorted().toList();
      if (days.size() < 3) {
        continue;
      }
      int widest = widestRecentGapDays(days);
      LocalDate last = days.get(days.size() - 1);
      int elapsed = (int) java.time.temporal.ChronoUnit.DAYS.between(last, today);
      if (elapsed > widest) {
        overdue.add(
            new DataStatusResponse.MonthlyDividendPayoutOverdueRow(
                names.getOrDefault(entry.getKey(), String.valueOf(entry.getKey())),
                last,
                elapsed,
                widest));
      }
    }
    overdue.sort((left, right) -> Integer.compare(right.elapsedDays(), left.elapsedDays()));
    return overdue.stream().limit(5).toList();
  }

  /**
   * 최근 지급 간격만 본다. 지급 주기가 바뀐 종목은 옛 간격이 기준을 무력화한다.
   *
   * <p>실측 2026-08-23: TIGER 리츠부동산인프라는 2020-02 ~ 2022-11 에 <b>분기</b> 배당이었다(간격 88~94 일). 2022-11 부터
   * 월배당으로 바뀌어 최근 12 간격은 27~34 일인데, 전체 최대인 94 일을 기준으로 쓰면 3 개월이 비어도 지연으로 잡히지 않는다. 최근 12 간격만 보면 기준이 34
   * 일이 된다.
   *
   * <p>나머지 7 종목은 최근 12 간격의 최대가 전체 최대와 같거나 1 일 차이라 판정이 달라지지 않는다 &mdash; 즉 이 변경은 주기가 바뀐 종목에만 듣는다.
   *
   * <p>12 라는 수는 예상 월배당의 "1 년 평균" 이 쓰는 건수와 같다({@code MonthlyDividendPayoutService} 의 {@code
   * limit(12)}). 같은 자료를 두 곳이 다른 창으로 보면 설명하기 어려워진다.
   */
  static int widestRecentGapDays(List<LocalDate> sortedDays) {
    int from = Math.max(1, sortedDays.size() - RECENT_PAYOUT_GAP_WINDOW);
    int widest = 0;
    for (int index = from; index < sortedDays.size(); index++) {
      widest =
          Math.max(
              widest,
              (int)
                  java.time.temporal.ChronoUnit.DAYS.between(
                      sortedDays.get(index - 1), sortedDays.get(index)));
    }
    return widest;
  }

  /** 지연 판정에 쓰는 최근 간격 개수. 예상 월배당의 "1년 평균" 과 같은 창이다. */
  private static final int RECENT_PAYOUT_GAP_WINDOW = 12;

  private List<DataStatusResponse.ZeroVolumeChangedCloseRow> toChangedCloseRows(
      List<net.luversof.api.stock.domain.ZeroVolumeChangedClose> rows) {
    if (rows == null || rows.isEmpty()) {
      return List.of();
    }
    Map<UUID, String> names = new HashMap<>();
    stockItemRepository
        .findAllById(rows.stream().map(row -> row.stockItemId()).filter(Objects::nonNull).toList())
        .forEach(item -> names.put(item.getId(), item.getName()));
    return withNames(rows, names);
  }

  private List<DataStatusResponse.PriceLimitBreachRow> toBreachRows(
      List<net.luversof.api.stock.domain.PriceLimitBreachRow> rows) {
    if (rows == null || rows.isEmpty()) {
      return List.of();
    }
    Map<UUID, String> names = new HashMap<>();
    stockItemRepository
        .findAllById(rows.stream().map(row -> row.stockItemId()).filter(Objects::nonNull).toList())
        .forEach(item -> names.put(item.getId(), item.getName()));
    return withBreachNames(rows, names);
  }

  /**
   * 가격제한폭 초과 행에 종목 이름과 변동률을 붙인다.
   *
   * <p>변동률은 화면이 다시 계산하지 않게 여기서 낸다 - 같은 식이 두 곳에 있으면 갈라진다. 직전 종가가 0 이면 비율이 없으므로 0 으로 둔다(조회가 이미 {@code
   * > 0} 으로 걸러 여기 오지 않는다).
   */
  static List<DataStatusResponse.PriceLimitBreachRow> withBreachNames(
      List<net.luversof.api.stock.domain.PriceLimitBreachRow> rows, Map<UUID, String> names) {
    if (rows == null || rows.isEmpty()) {
      return List.of();
    }
    Map<UUID, String> resolved = names != null ? names : Map.of();
    return rows.stream()
        .map(
            row -> {
              java.math.BigDecimal previous = row.previousClosePrice();
              double changePercent =
                  previous != null && previous.signum() != 0 && row.closePrice() != null
                      ? row.closePrice()
                              .subtract(previous)
                              .divide(previous, 8, java.math.RoundingMode.HALF_UP)
                              .doubleValue()
                          * 100.0
                      : 0.0;
              int gapDays =
                  row.previousTradeDate() != null && row.tradeDate() != null
                      ? (int)
                          java.time.temporal.ChronoUnit.DAYS.between(
                              row.previousTradeDate(), row.tradeDate())
                      : 0;
              return new DataStatusResponse.PriceLimitBreachRow(
                  resolved.getOrDefault(row.stockItemId(), String.valueOf(row.stockItemId())),
                  row.tradeDate(),
                  row.previousTradeDate(),
                  previous,
                  row.closePrice(),
                  changePercent,
                  gapDays);
            })
        .toList();
  }

  /**
   * 조회 결과에 종목 이름을 붙인다.
   *
   * <p>이름을 못 찾으면 id 를 그대로 쓴다 &mdash; 이름이 없다고 행을 숨기면 정작 이상한 데이터를 못 보게 된다. 실측 2026-08-23 의 유일한 행은 쌍방울
   * 2025-05-08 로, 종가가 13,450 에서 2,690 으로 정확히 1/5 이 됐다(액면분할이며 수집 오류가 아니다).
   */
  static List<DataStatusResponse.ZeroVolumeChangedCloseRow> withNames(
      List<net.luversof.api.stock.domain.ZeroVolumeChangedClose> rows, Map<UUID, String> names) {
    if (rows == null || rows.isEmpty()) {
      return List.of();
    }
    Map<UUID, String> resolved = names != null ? names : Map.of();
    return rows.stream()
        .map(
            row ->
                new DataStatusResponse.ZeroVolumeChangedCloseRow(
                    resolved.getOrDefault(row.stockItemId(), String.valueOf(row.stockItemId())),
                    row.tradeDate(),
                    row.previousClosePrice(),
                    row.closePrice()))
        .toList();
  }
}
