package net.luversof.api.stock.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.api.stock.domain.MonthlyDividendPayout;
import net.luversof.api.stock.domain.MonthlyDividendProfile;
import net.luversof.api.stock.domain.StockDailyClosePrice;
import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.repository.MonthlyDividendPayoutQuery;
import net.luversof.api.stock.repository.MonthlyDividendPayoutRepository;
import net.luversof.api.stock.repository.MonthlyDividendProfileRepository;
import net.luversof.api.stock.repository.StockDailyClosePriceQuery;
import net.luversof.api.stock.repository.StockItemRepository;
import net.luversof.api.stock.repository.StockPriceHistoryRepository;
import net.luversof.api.stock.service.MonthlyDividendPayoutService.SnapshotStats;
import net.luversof.api.stock.web.dto.response.MonthlyDividendCatalogResponse;
import net.luversof.api.stock.web.dto.response.MonthlyDividendCatalogResponse.PeriodReturnView;

/**
 * 월배당 ETF 목록 &mdash; 등록된 프로필과 지급 이력으로 만드는 <b>종목 단위</b> 정보.
 *
 * <p>사용자 요청 2026-09-21: 보유하지 않은 월배당 ETF 도 등록해 두고 효율 · 과세 비중으로 견주고 싶다. 시뮬레이터의 월배당 탭은 내가 받을 배당을 다루므로
 * 보유 종목만 두고, 종목 정보는 이 목록이 맡는다.
 *
 * <p>수익률은 주당 배당 &divide; 현재가다 &mdash; 수량이 없어도 성립한다.
 */
@Service
public class MonthlyDividendCatalogService {

  /** 화면이 고를 수 있는 기간(개월). 사용자 결정 2026-09-21. */
  private static final int[] PERIOD_MONTHS = {1, 3, 6, 12};

  private static final ZoneId MARKET_ZONE_ID = ZoneId.of("Asia/Seoul");

  @Autowired private MonthlyDividendProfileRepository monthlyDividendProfileRepository;

  @Autowired private MonthlyDividendPayoutRepository monthlyDividendPayoutRepository;

  @Autowired private MonthlyDividendPayoutQuery monthlyDividendPayoutQuery;

  @Autowired private MonthlyDividendPayoutService monthlyDividendPayoutService;

  @Autowired private StockItemRepository stockItemRepository;

  @Autowired private StockPriceService stockPriceService;

  @Autowired private StockPriceHistoryRepository stockPriceHistoryRepository;

  @Autowired private StockDailyClosePriceQuery stockDailyClosePriceQuery;

  /** 카탈로그가 읽는 시세 창(개월) - 가장 긴 계산 기간 12 개월 + 휴장 · 거래 공백 여유 3 개월. */
  static final int CALCULATION_WINDOW_MONTHS = 15;

  /** 등록된 프로필 전부(표시 순서대로). activeOnly 면 활성 프로필만. */
  public List<MonthlyDividendCatalogResponse> findCatalog(Boolean activeOnly) {
    List<MonthlyDividendProfile> profiles =
        Boolean.TRUE.equals(activeOnly)
            ? monthlyDividendProfileRepository.findByActiveOrderByDisplayOrderAscUpdatedDateDesc(
                true)
            : monthlyDividendProfileRepository.findAllByOrderByDisplayOrderAscUpdatedDateDesc();
    if (profiles.isEmpty()) {
      return List.of();
    }

    // 종목 · 현재가는 한 번에 읽는다(행마다 읽으면 행 수만큼 질의가 나간다).
    Set<UUID> stockItemIds = new HashSet<>();
    profiles.forEach(
        profile -> {
          if (profile.getStockItemId() != null) {
            stockItemIds.add(profile.getStockItemId());
          }
        });
    Map<UUID, StockItem> stockItemById = new HashMap<>();
    stockItemRepository
        .findAllById(stockItemIds)
        .forEach(item -> stockItemById.put(item.getId(), item));
    Map<UUID, StockDailyClosePrice> priceById = stockPriceService.getLatestPrices(stockItemIds);
    // 지급 이력 · 시세 이력도 한 번에 읽는다. 실측 2026-09-23: 종목(21)마다 지급 이력 두 번(통계 · 추세) · 시세 한 번씩 63 번 왕복해
    // 이 응답이 190~230ms 였고, 월배당 ETF 화면 본문 280ms · 시뮬레이터 월배당 216ms 의 대부분이었다.
    Map<UUID, List<MonthlyDividendPayout>> payoutsById = new HashMap<>();
    Map<UUID, List<StockDailyClosePrice>> priceHistoryById = new HashMap<>();
    Map<UUID, LocalDate> firstTradeDateById = new HashMap<>();
    // 기간 수익률용 원주가(2026-10-02, PeriodReturnCalculator.preferRaw).
    Map<UUID, List<StockDailyClosePrice>> rawHistoryById = new HashMap<>();
    if (!stockItemIds.isEmpty()) {
      for (MonthlyDividendPayout payout :
          monthlyDividendPayoutQuery.findByStockItemIdInOrderByPayDateDescRecordDateDesc(
              stockItemIds)) {
        payoutsById.computeIfAbsent(payout.getStockItemId(), id -> new ArrayList<>()).add(payout);
      }
      String ids =
          stockItemIds.stream()
              .map(UUID::toString)
              .collect(java.util.stream.Collectors.joining(","));
      // 기간 수익률 · 위험 지표는 최근 12 개월만 쓴다 - 2 년치를 다 읽으면 행이 두 배다(실측 2026-09-23: 약 1 만 행).
      // 기초일이 휴장이면 그 전 거래일을 찾으므로 3 개월 여유를 둔다.
      LocalDate fromDate = LocalDate.now(MARKET_ZONE_ID).minusMonths(CALCULATION_WINDOW_MONTHS);
      for (StockDailyClosePrice row :
          stockDailyClosePriceQuery.findDailyClosePricesForItems(ids, fromDate)) {
        priceHistoryById.computeIfAbsent(row.stockItemId(), id -> new ArrayList<>()).add(row);
      }
      rawHistoryById.putAll(stockPriceService.getRawClosePricesForItems(ids, fromDate));
      for (StockDailyClosePrice row :
          stockPriceHistoryRepository.findFirstTradeDatesForItems(ids)) {
        firstTradeDateById.put(row.stockItemId(), row.tradeDate());
      }
    }

    List<MonthlyDividendCatalogResponse> rows = new ArrayList<>();
    for (MonthlyDividendProfile profile : profiles) {
      UUID stockItemId = profile.getStockItemId();
      StockItem stockItem = stockItemId != null ? stockItemById.get(stockItemId) : null;
      List<MonthlyDividendPayout> payouts =
          stockItemId != null ? payoutsById.getOrDefault(stockItemId, List.of()) : List.of();
      SnapshotStats stats =
          stockItemId != null
              ? monthlyDividendPayoutService.computeSnapshotStatsFrom(payouts)
              : null;
      MonthlyDividendPayout latestPayout = payouts.isEmpty() ? null : payouts.get(0);
      StockDailyClosePrice price = stockItemId != null ? priceById.get(stockItemId) : null;

      // 기간 수익률은 시세 이력으로 낸다(저장하지 않는다 - 매일 바뀌는 계산값이다).
      List<StockDailyClosePrice> priceHistory =
          stockItemId != null ? priceHistoryById.getOrDefault(stockItemId, List.of()) : List.of();
      List<PeriodReturnCalculator.PayoutPoint> payoutPoints =
          payouts.stream()
              .map(
                  payout ->
                      new PeriodReturnCalculator.PayoutPoint(
                          payout.getRecordDate(), payout.getDividendAmountPerShare()))
              .toList();
      LocalDate today = LocalDate.now(MARKET_ZONE_ID);
      List<StockDailyClosePrice> periodHistory =
          PeriodReturnCalculator.preferRaw(
              stockItemId != null ? rawHistoryById.getOrDefault(stockItemId, List.of()) : List.of(),
              priceHistory);
      List<PeriodReturnView> periodReturns = new ArrayList<>();
      for (int months : PERIOD_MONTHS) {
        PeriodReturnCalculator.PeriodReturn computed =
            PeriodReturnCalculator.compute(periodHistory, payoutPoints, today, months);
        if (computed != null) {
          periodReturns.add(
              new PeriodReturnView(
                  computed.months(),
                  computed.baseDate(),
                  computed.priceReturnPct(),
                  computed.totalReturnPct()));
        }
      }
      // 위험 지표는 1 년 기준이다(사용자 결정 2026-09-22). 추천 점수에는 안 섞고 표에 보여 주기만 한다.
      // 가격만 보는 지표라 기간 수익률과 같은 원주가 우선 시계열을 쓴다(2026-10-02). 수정 종가는 한꺼번에 받은 옛 구간만 분배금이 깎여
      // 있어 그 경계에 가짜 하루 변동이 끼었다 - 15 개월 창에서 1% 넘게 어긋난 날 160 일, 0094M0 2026-03-25 수정 +18.65% / 실제
      // +1.92%.
      RiskMetricsCalculator.RiskMetrics risk =
          RiskMetricsCalculator.compute(periodHistory, today, RISK_MONTHS);

      // 이력 시작일은 창과 무관하게 실제 첫 거래일이다(화면의 "YYYY-MM-DD 부터").
      LocalDate priceHistoryStartDate =
          stockItemId != null ? firstTradeDateById.get(stockItemId) : null;

      // 분배금 추세는 지급 이력만으로 난다. null 은 여기서 0 으로 맞춰 평균이 computeSnapshotStats 와 같게 나오게 한다.
      PayoutTrendCalculator.PayoutTrend payoutTrend =
          PayoutTrendCalculator.compute(
              payouts.stream().map(payout -> safe(payout.getDividendAmountPerShare())).toList());

      PayoutTrendCalculator.PayoutCuts payoutCuts =
          PayoutTrendCalculator.countCuts(
              payouts.stream().map(payout -> safe(payout.getDividendAmountPerShare())).toList());

      BigDecimal currentPrice = price != null ? safe(price.closePrice()) : BigDecimal.ZERO;
      BigDecimal averagePerShare =
          stats != null ? safe(stats.averagePerShare1y()) : BigDecimal.ZERO;
      BigDecimal taxableRatio = stats != null ? safe(stats.taxableBaseRatio1y()) : BigDecimal.ZERO;
      BigDecimal monthlyYieldPct = percent(averagePerShare, currentPrice);

      rows.add(
          new MonthlyDividendCatalogResponse(
              stockItemId,
              stockItem != null ? stockItem.getSymbol() : "",
              stockItem != null ? stockItem.getName() : "",
              profile.getPayoutWindow(),
              profile.getSourceUrl(),
              profile.getLastVerifiedDate(),
              Boolean.TRUE.equals(profile.getActive()),
              profile.getDisplayOrder(),
              payouts.size(),
              latestPayout != null ? latestPayout.getRecordDate() : null,
              latestPayout != null ? latestPayout.getPayDate() : null,
              stats != null ? safe(stats.latestPerShare()) : BigDecimal.ZERO,
              averagePerShare,
              taxableRatio,
              averagePerShare
                  .multiply(taxableRatio)
                  .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP),
              currentPrice,
              price != null ? price.tradeDate() : null,
              monthlyYieldPct,
              monthlyYieldPct.multiply(BigDecimal.valueOf(12)).setScale(2, RoundingMode.HALF_UP),
              priceHistoryStartDate,
              List.copyOf(periodReturns),
              payoutTrend != null ? payoutTrend.recentAveragePerShare() : null,
              payoutTrend != null ? payoutTrend.changePct() : null,
              risk != null ? risk.maxDrawdownPct() : null,
              risk != null ? risk.volatilityPct() : null,
              risk != null ? risk.fromDate() : null,
              profile.getTotalExpenseRatioPct(),
              profile.getListingDate(),
              payoutCuts != null ? payoutCuts.cutCount() : null,
              payoutCuts != null ? payoutCuts.pairCount() : null));
    }
    return rows;
  }

  /** 위험 지표를 내는 기간(개월). 사용자 결정 2026-09-22. */
  private static final int RISK_MONTHS = 12;

  private BigDecimal safe(BigDecimal value) {
    return value != null ? value : BigDecimal.ZERO;
  }

  /** 분모가 0 이하면 0 - 시세를 못 받은 종목은 수익률을 지어내지 않는다. */
  private BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
    if (numerator == null || denominator == null || denominator.signum() <= 0) {
      return BigDecimal.ZERO;
    }

    return numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, 2, RoundingMode.HALF_UP);
  }
}
